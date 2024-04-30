/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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

package ee.fhir.fhirest.validation;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ee.fhir.fhirest.core.api.resource.OperationInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceBeforeSaveInterceptor;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirServerException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.service.conformance.HapiContextHolder;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
      throw new FhirException(FhirestIssue.FEST_021, "message", e.getMessage());
    }
  }

  private void validateType(String expectedType, String resourceType) {
    if (!resourceType.equals(expectedType)) {
      throw new FhirException(FhirestIssue.FEST_022, "expected", expectedType, "actual", resourceType);
    }
  }

  private void validateProfile(ResourceContent content) {
    if (hapiContextHolder.getHapiContext() == null) {
      throw new FhirServerException("fhir context initialization error");
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
        throw new FhirException(FhirestIssue.FEST_023, "message", e.getMessage());
      }
      log.error("exception during profile validation", e);
      throw new FhirServerException("exception during profile validation: " + e.getMessage());
    }
  }

  private boolean isError(ResultSeverityEnum level) {
    return level == ResultSeverityEnum.ERROR || level == ResultSeverityEnum.FATAL;
  }

}
