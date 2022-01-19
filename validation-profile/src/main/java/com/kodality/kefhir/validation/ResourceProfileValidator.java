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
package com.kodality.kefhir.validation;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.core.api.resource.OperationInterceptor;
import com.kodality.kefhir.core.api.resource.ResourceBeforeSaveInterceptor;
import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.exception.FhirServerException;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.structure.api.ParseException;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.api.ResourceRepresentation;
import com.kodality.kefhir.structure.service.HapiContextHolder;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import java.io.ByteArrayInputStream;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;

import static java.util.stream.Collectors.toList;

@Singleton
public class ResourceProfileValidator extends ResourceBeforeSaveInterceptor implements ConformanceUpdateListener, OperationInterceptor {
  @Inject
  private ResourceFormatService resourceFormatService;
  @Inject
  private ResourceFormatService representationService;
  @Inject
  private HapiContextHolder hapiContextHolder;

  public ResourceProfileValidator() {
    super(ResourceBeforeSaveInterceptor.INPUT_VALIDATION);
  }

  @Override
  public void updated() {
//    if (definition == null) {
//      return;
//    }
//    try {
//      fhirContext = SimpleWorkerContext.fromDefinitions(definition);
//      ((BaseWorkerContext) fhirContext).setCanRunWithoutTerminology(true);
//    } catch (IOException | FHIRException e) {
//      throw new RuntimeException("fhir fhir ");
//    }
//
//  IWorkerContext fhirContext = SimpleWorkerContext.fromDefinitions(definition);
//((BaseWorkerContext) fhirContext).setCanRunWithoutTerminology(true);
  }

  @Override
  public void handle(String level, String operation, ResourceContent parameters) {
    if (StringUtils.isEmpty(parameters.getValue())) {
      return;
    }
    runValidation(ResourceType.Parameters.name(), parameters);
  }

  @Override
  public void handle(ResourceId id, ResourceContent content, String interaction) {
    String resourceType = id.getResourceType();
    runValidation(resourceType, content);

  }

  private void runValidation(String resourceType, ResourceContent content) {
    StructureDefinition definition = ConformanceHolder.getDefinition(resourceType);
    if (definition == null) {
      throw new FhirServerException(500, "definition for " + resourceType + " not found");
    }
    if (hapiContextHolder.getHapiContext() == null) {
      throw new FhirServerException(500, "fhir context initialization error");
    }
    List<SingleValidationMessage> errors = validate(resourceType, content);
    errors = errors.stream().filter(m -> isError(m.getSeverity())).collect(toList());
    if (!errors.isEmpty()) {
      throw new FhirException(400, errors.stream().map(msg -> {
        OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent();
        issue.setCode(IssueType.INVALID);
//        issue.setSeverity(severity(msg));
        issue.setSeverity(severity(msg.getSeverity()));
        issue.setDetails(new CodeableConcept().setText(msg.getMessage()));
        issue.addLocation(msg.getLocationString());
        return issue;
      }).collect(toList()));
    }
  }

  private boolean isError(ResultSeverityEnum level) {
    return level == ResultSeverityEnum.ERROR
        || level == ResultSeverityEnum.FATAL;
  }
//  private boolean isError(org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity level) {
//    return level == org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity.ERROR
//        || level == org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity.FATAL;
//  }

  private IssueSeverity severity(ResultSeverityEnum msg) {
    try {
      return IssueSeverity.fromCode(msg.getCode());
    } catch (FHIRException e) {
      throw new RuntimeException("спасибо вам.", e);
    }
  }

  private List<SingleValidationMessage> validate(String resourceType, ResourceContent content) {
//    Element element;
//    List<ValidationMessage> messages = new ArrayList<ValidationMessage>();
    try {
      FhirValidator validator = (FhirValidator) hapiContextHolder.getContext().newValidator();
//      validator.setAnyExtensionsAllowed(true);
      ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes());
      ValidationResult vr = validator.validateWithResult(resourceFormatService.parse(content));
      return vr.getMessages();
//      element = validator.validate(null, messages, input, getFhirFormat(content));
    } catch (Exception e) {
      throw new RuntimeException(":/", e);
    }
//    if (element != null && !element.getType().equals(resourceType)) {
//      String msg = "was expecting " + resourceType + " but found " + element.getType();
//      throw new FhirException(400, IssueType.INVALID, msg);
//    }
//    return messages;
  }

  private FhirFormat getFhirFormat(ResourceContent content) {
    String ct = StringUtils.substringBefore(content.getContentType(), ";");
    ResourceRepresentation repr =
        representationService.findPresenter(ct).orElseThrow(() -> new ParseException("unknown format"));
    return repr.getFhirFormat();
  }

}
