/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ee.fhir.fhirest.core.service.resource;

import ee.fhir.fhirest.core.api.resource.ResourceBeforeSearchInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceSearchHandler;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirServerException;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.SearchCriterion;
import ee.fhir.fhirest.core.model.search.SearchCriterionBuilder;
import ee.fhir.fhirest.core.model.search.SearchResult;
import ee.fhir.fhirest.core.service.FhirPath;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.core.util.MapUtil;
import ee.fhir.fhirest.core.util.ResourceUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Enumeration;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.SearchParameter;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
public class ResourceSearchService {
  private static final String INCLUDE_ALL = "*";
  private static final String ITERATE = "iterate";

  private final Map<String, ResourceSearchHandler> searchHandlers;
  private final List<ResourceBeforeSearchInterceptor> beforeSearchInterceptors;
  private final ResourceStorageService storageService;
  private final FhirPath fhirPath;

  public ResourceSearchService(List<ResourceSearchHandler> searchHandlers, List<ResourceBeforeSearchInterceptor> beforeSearchInterceptors,
                               ResourceStorageService storageService, FhirPath fhirPath) {
    this.searchHandlers = searchHandlers.stream().collect(Collectors.toMap(s -> s.getResourceType(), s -> s));
    this.beforeSearchInterceptors = beforeSearchInterceptors;
    this.storageService = storageService;
    this.fhirPath = fhirPath;
  }

  private ResourceSearchHandler getSearchHandler(String resourceType) {
    return searchHandlers.getOrDefault(resourceType, searchHandlers.get(ResourceSearchHandler.DEFAULT));
  }

  public SearchResult search(String resourceType, String... params) {
    return search(resourceType, MapUtil.toMultimap((Object[]) params));
  }

  public SearchResult search(String resourceType, Map<String, List<String>> params) {
    return search(SearchCriterionBuilder.parse(params, resourceType));
  }

  public SearchResult search(SearchCriterion criteria) {
    ResourceSearchHandler searchHandler = getSearchHandler(criteria.getType());
    if (searchHandler == null) {
      throw new FhirServerException(500, "search module not installed");
    }
    beforeSearchInterceptors.forEach(i -> i.handle(criteria));
    SearchResult result = searchHandler.search(criteria);
    List<ResourceId> loadIds = result.getEntries().stream().filter(e -> e.getContent() == null).map(e -> (ResourceId) e.getId()).collect(toList());
    Map<String, ResourceVersion> versions = storageService.load(loadIds).stream().collect(toMap(v -> v.getId().getResourceReference(), v -> v));
    result.getEntries()
        .stream()
        .filter(e -> e.getContent() == null)
        .forEach(e -> {
          ResourceVersion version = versions.get(e.getId().getResourceReference());
          e.setId(version.getId());
          e.setContent(version.getContent());
          e.setAuthor(version.getAuthor());
        });
    include(result, criteria);
    revInclude(result, criteria);
    return result;
  }

  private void include(SearchResult result, SearchCriterion criteria) {
    criteria.getResultParams(SearchCriterion._INCLUDE).forEach(ip -> ip.getValues().forEach(includeKey -> {
      String[] includeTokens = SearchUtil.parseInclude(includeKey);
      String resourceType = includeTokens[0];
      String searchParam = includeTokens[1];
      String targetType = includeTokens[2];
      List<String> expressions = findReferenceParams(resourceType, searchParam).map(SearchParameter::getExpression).toList();
      if (CollectionUtils.isEmpty(expressions)) {
        throw new FhirException(400, IssueType.INVALID, "Could not find SearchParameter with code '" + searchParam + "' for resource '" + resourceType + "'");
      }
      List<ResourceVersion> entries = new ArrayList<>(result.getEntries());
      if (ITERATE.equals(ip.getModifier())) {
        entries.addAll(result.getIncludes());
      }
      List<ResourceId> resourceIds = new ArrayList<>();
      entries.stream()
          .filter(e -> e.getId().getResourceType().equals(resourceType))
          .forEach(entry -> expressions.stream()
              .flatMap(expr -> evaluate(entry, expr))
              .map(ref -> ResourceUtil.parseReference(ref.getReference()))
              .filter(reference -> targetType == null || reference.getResourceType().equals(targetType))
              .filter(reference -> !contains(result, reference))
              .forEach(resourceIds::add)
          );
      result.addIncludes(storageService.load(resourceIds));
    }));
  }

  private void revInclude(SearchResult result, SearchCriterion criteria) {
    if (CollectionUtils.isEmpty(criteria.getResultParams(SearchCriterion._REVINCLUDE))) {
      return;
    }
    List<String> entryRefs = result.getEntries().stream().map(e -> e.getId().getResourceReference()).collect(toList());
    if (CollectionUtils.isEmpty(entryRefs)) {
      return;
    }
    criteria.getResultParams(SearchCriterion._REVINCLUDE).forEach(ip -> ip.getValues().forEach(includeKey -> {
      String[] includeTokens = SearchUtil.parseInclude(includeKey);
      String resourceType = includeTokens[0];
      String searchParam = includeTokens[1];
      String targetType = includeTokens[2];
      if (searchParam.equals(INCLUDE_ALL)) {
        throw new FhirServerException(501, "revinclude '*' not currently supported");
      }
      List<String> references = entryRefs;
      if (!ITERATE.equals(ip.getModifier()) && targetType != null && !targetType.equals(criteria.getType())) {
        return;
      }
      if (ITERATE.equals(ip.getModifier())) {
        List<String> referenceTypes = findReferenceParams(resourceType, searchParam)
            .flatMap(sp -> sp.getTarget().stream()).map(Enumeration::getCode)
            .filter(t -> targetType == null || t.equals(targetType))
            .toList();
        if (CollectionUtils.isEmpty(referenceTypes)) {
          return;
        }
        references = result.getIncludes().stream().filter(r -> referenceTypes.contains(r.getId().getResourceType()))
            .map(e -> e.getId().getResourceReference()).toList();
        if (CollectionUtils.isEmpty(references)) {
          return;
        }
      }
      Map<String, List<String>> revSearch = new HashMap<>(1);
      revSearch.put(searchParam, Collections.singletonList(StringUtils.join(references, ",")));
      revSearch.put(SearchCriterion._COUNT, Collections.singletonList("1000"));// would kill search anyway if more
      result.addIncludes(search(resourceType, revSearch).getEntries());
    }));
  }

  private boolean contains(SearchResult result, VersionId reference) {
    return Stream.concat(result.getEntries().stream(), result.getIncludes().stream()).anyMatch(e -> {
      //TODO: if reference is non-versioned - last version must be included.
      //      need to make sure if included resource is already last version or not.
      return reference.getVersion() == null
          ? e.getId().getResourceReference().equals(reference.getResourceReference())
          : e.getId().getReference().equals(reference.getReference());
    });
  }

  private Stream<org.hl7.fhir.r5.model.Reference> evaluate(ResourceVersion entry, String expr) {
    return fhirPath.<org.hl7.fhir.r5.model.Reference>evaluate(entry.getContent().getValue(), expr).stream();
  }

  private Stream<SearchParameter> findReferenceParams(String resourceType, String param) {
    return ConformanceHolder.getSearchParams(resourceType)
        .stream()
        .filter(sp -> param.equals(INCLUDE_ALL) || sp.getCode().equals(param))
        .filter(sp -> sp.getType() == SearchParamType.REFERENCE);
  }

}
