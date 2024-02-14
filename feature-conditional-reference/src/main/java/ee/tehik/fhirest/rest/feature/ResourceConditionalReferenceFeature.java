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
package ee.tehik.fhirest.rest.feature;

import ee.tehik.fhirest.core.api.resource.OperationInterceptor;
import ee.tehik.fhirest.core.api.resource.ResourceBeforeSaveInterceptor;
import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.core.model.ResourceId;
import ee.tehik.fhirest.core.model.search.SearchCriterion;
import ee.tehik.fhirest.core.model.search.SearchCriterionBuilder;
import ee.tehik.fhirest.core.model.search.SearchResult;
import ee.tehik.fhirest.core.service.resource.ResourceSearchService;
import ee.tehik.fhirest.structure.api.ResourceContent;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import ee.tehik.fhirest.structure.util.ResourcePropertyUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Resource;

/**
 * https://www.hl7.org/fhir/http.html#transaction
 * Conditional Reference implementation.
 * originally should only apply to transaction, but we thought might be global..
 */
@Component
public class ResourceConditionalReferenceFeature extends ResourceBeforeSaveInterceptor implements OperationInterceptor {
  @Inject
  private ResourceFormatService representationService;
  @Inject
  private Provider<ResourceSearchService> resourceSearchService;

  public ResourceConditionalReferenceFeature() {
    super(ResourceBeforeSaveInterceptor.NORMALIZATION);
  }

  @Override
  public void handle(String level, String operation, ResourceContent parameters) {
    replaceRefs(parameters);
  }

  @Override
  public void handle(ResourceId id, ResourceContent content, String interaction) {
    replaceRefs(content);
  }

  private void replaceRefs(ResourceContent content) {
    if (StringUtils.isEmpty(content.getValue())) {
      return;
    }
    Resource resource = representationService.parse(content.getValue());
    ResourcePropertyUtil.findProperties(resource, Reference.class).forEach(reference -> replaceReference(reference));
    content.setValue(representationService.compose(resource, content.getContentType()).getValue());
  }

  private void replaceReference(Reference reference) {
    String uri = reference.getReference();
    if (uri == null || !uri.contains("?")) {
      return;
    }
    String resourceType = StringUtils.substringBefore(uri, "?");
    String query = StringUtils.substringAfter(uri, "?");

    SearchCriterion criteria = SearchCriterionBuilder.parse(query, resourceType);
    criteria.getResultParams().clear();
    SearchResult result = resourceSearchService.get().search(criteria);
    if (result.getTotal() != 1) {
      throw new FhirException(400, IssueType.PROCESSING, "found " + result.getTotal() + " resources by " + uri);
    }
    reference.setReference(result.getEntries().get(0).getId().getResourceReference());
  }

}
