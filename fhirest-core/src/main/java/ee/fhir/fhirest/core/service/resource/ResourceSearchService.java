/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ee.fhir.fhirest.core.service.resource;

import ee.fhir.fhirest.core.api.resource.ResourceBeforeSearchInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceSearchHandler;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirServerException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
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
import org.hl7.fhir.r5.model.SearchParameter;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * <p>Service responsible for resource search interactions.</p>
 * <p>Depends on {@link ResourceSearchHandler} implementations</p>
 */
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

  /**
   * @param params array of alternating key-values
   * @see ResourceSearchService#search(SearchCriterion)
   */
  public SearchResult search(String resourceType, String... params) {
    return search(resourceType, MapUtil.toMultimap((Object[]) params));
  }

  /**
   * @param params Multivalued map of query parameters
   * @see ResourceSearchService#search(SearchCriterion)
   */
  public SearchResult search(String resourceType, Map<String, List<String>> params) {
    return search(SearchCriterionBuilder.parse(params, resourceType));
  }

  /**
   * @param criteria
   * @return Search result resources and includes
   */
  public SearchResult search(SearchCriterion criteria) {
    ResourceSearchHandler searchHandler = getSearchHandler(criteria.getType());
    if (searchHandler == null) {
      throw new FhirServerException("search module not installed");
    }
    beforeSearchInterceptors.forEach(i -> i.handle(criteria));
    SearchResult result = searchHandler.search(criteria);
    List<VersionId> loadIds = result.getEntries().stream().filter(e -> e.getContent() == null).map(ResourceVersion::getId).collect(toList());
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
        throw new FhirException(FhirestIssue.FEST_024, "param", searchParam, "resource", resourceType);
      }
      List<ResourceVersion> entries = new ArrayList<>(result.getEntries());
      if (ITERATE.equals(ip.getModifier())) {
        entries.addAll(result.getIncludes());
      }
      List<VersionId> resourceIds = new ArrayList<>();
      entries.stream()
          .filter(e -> e.getId().getResourceType().equals(resourceType))
          .forEach(entry -> expressions.stream()
              .flatMap(expr -> evaluate(entry, expr))
              .map(ref -> ResourceUtil.parseReference(ref.getReference()))
              .filter(reference -> targetType == null || reference.getResourceType().equals(targetType))
              .filter(reference -> !contains(result, reference))
              .forEach(resourceIds::add)
          );
      List<VersionId> uniqueResourceIds = ResourceUtil.filterUnique(resourceIds);
      result.addIncludes(storageService.load(uniqueResourceIds));
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
        throw new FhirException(FhirestIssue.FEST_001, "desc", "revinclude '*' not currently supported");
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
