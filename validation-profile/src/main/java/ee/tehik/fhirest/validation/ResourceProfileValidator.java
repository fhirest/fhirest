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
package ee.tehik.fhirest.validation;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ee.tehik.fhirest.core.api.resource.OperationInterceptor;
import ee.tehik.fhirest.core.api.resource.ResourceBeforeSaveInterceptor;
import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.core.exception.FhirServerException;
import ee.tehik.fhirest.core.model.ResourceId;
import ee.tehik.fhirest.core.service.conformance.HapiContextHolder;
import ee.tehik.fhirest.structure.api.ResourceContent;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import jakarta.inject.Inject;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

@ConditionalOnProperty(value = "fhirest.validation-profile.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class ResourceProfileValidator extends ResourceBeforeSaveInterceptor implements OperationInterceptor {
  @Inject
  private ResourceFormatService resourceFormatService;
  @Inject
  private HapiContextHolder hapiContextHolder;

  public ResourceProfileValidator() {
    super(ResourceBeforeSaveInterceptor.INPUT_VALIDATION);
  }

  @Override
  public void handle(String level, String operation, ResourceContent parameters) {
    if (StringUtils.isEmpty(parameters.getValue())) {
      return;
    }

    // some operations ($transform) accept non-default-spec resources, thus parse fails
    // runValidation(ResourceType.Parameters.name(), parameters);
    validateProfile(parameters);
  }

  @Override
  public void handle(ResourceId id, ResourceContent content, String interaction) {
    String resourceType = id.getResourceType();
    runValidation(resourceType, content);
  }

  private void runValidation(String resourceType, ResourceContent content) {
    Resource resource = validateParse(content);
    validateType(resourceType, resource.getResourceType().name());
    validateProfile(content);
  }

  private Resource validateParse(ResourceContent content) {
    try {
      return resourceFormatService.parse(content);
    } catch (Exception e) {
      throw new FhirException(400, IssueType.STRUCTURE, "error during resource parse: " + e.getMessage());
    }
  }

  private void validateType(String expectedType, String resourceType) {
    if (!resourceType.equals(expectedType)) {
      String msg = "was expecting " + expectedType + " but found " + resourceType;
      throw new FhirException(400, IssueType.INVALID, msg);
    }
  }

  private void validateProfile(ResourceContent content) {
    if (hapiContextHolder.getHapiContext() == null) {
      throw new FhirServerException(500, "fhir context initialization error");
    }
    try {
      List<SingleValidationMessage> errors = hapiContextHolder.getValidator().validateWithResult(content.getValue()).getMessages();
      errors = errors.stream().filter(m -> isError(m.getSeverity())).collect(toList());
      if (!errors.isEmpty()) {
        throw new FhirException(400, errors.stream().map(msg -> {
          OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent();
          issue.setCode(IssueType.INVALID);
          issue.setSeverity(IssueSeverity.fromCode(msg.getSeverity().getCode()));
          issue.setDetails(new CodeableConcept().setText(msg.getMessage()));
          issue.addLocation(msg.getLocationString());
          return issue;
        }).collect(toList()));
      }
    } catch (Exception e) {
      if (e instanceof FHIRException) {
        throw new FhirException(500, IssueType.INVALID, e.getMessage());
      }
      throw new RuntimeException("exception during profile validation: " + e.getMessage(), e);
    }
  }

  private boolean isError(ResultSeverityEnum level) {
    return level == ResultSeverityEnum.ERROR || level == ResultSeverityEnum.FATAL;
  }

}
