package ee.fhir.fhirest.rest.operation;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.hl7.fhir.r5.model.Enumerations.OperationParameterUse;
import org.hl7.fhir.r5.model.OperationDefinition;
import org.hl7.fhir.r5.model.OperationDefinition.OperationDefinitionParameterComponent;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.ResourceType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationParametersReader {
  private final ResourceFormatService resourceFormatService;

  public ResourceContent readOperationParameters(String operation, FhirestRequest req) {
    OperationDefinition opDef = req.getType() == null ? findBaseOperationDefinition(operation) : findOperationDefinition(operation, req.getType());

    if (req.getMethod().equals("GET")) {
      if (opDef.getAffectsState()) {
        throw new FhirException(FhirestIssue.FEST_011);
      }
      Parameters parameters = new Parameters();
      req.getParameters().forEach((k, v) -> parameters.addParameter(k, String.join(",", v)));
      return resourceFormatService.compose(parameters, "json");
    }

    Resource body = req.getBody() == null ? null : resourceFormatService.parse(req.getBody());
    if (body != null && body.getResourceType() == ResourceType.Parameters) {
      return new ResourceContent(req.getBody(), req.getContentTypeName());
    }

    List<OperationDefinitionParameterComponent> resourceParams =
        opDef.getParameter().stream().filter(p -> p.getUse() == OperationParameterUse.IN && p.getType() == FHIRTypes.RESOURCE).toList();
    if (body == null && !resourceParams.isEmpty()) {
      throw new FhirException(FhirestIssue.FEST_012);
    }
    if (body != null && resourceParams.size() != 1) {
      throw new FhirException(FhirestIssue.FEST_013);
    }
    String resourceParameterName = resourceParams.get(0).getName();

    Parameters parameters = new Parameters();
    req.getParameters().forEach((k, v) -> parameters.addParameter(k, String.join(",", v)));
    parameters.addParameter().setName(resourceParameterName).setResource(body);
    return resourceFormatService.compose(parameters, req.getContentTypeName());
  }

  private static OperationDefinition findBaseOperationDefinition(String operation) {
    CapabilityStatementRestResourceOperationComponent capabilityOp =
        ConformanceHolder.getCapabilityStatement().getRest().stream().filter(r -> r.getMode() == RestfulCapabilityMode.SERVER)
            .findFirst()
            .orElseThrow()
            .getOperation().stream().filter(op -> ("$" + op.getName()).equals(operation)).findFirst()
            .orElseThrow(() -> new FhirException(FhirestIssue.FEST_014, "operation", operation));
    OperationDefinition opDef = ConformanceHolder.getOperationDefinition(capabilityOp.getDefinition());
    if (opDef == null) {
      throw new FhirException(FhirestIssue.FEST_015, "operation", operation);
    }
    return opDef;
  }

  private static OperationDefinition findOperationDefinition(String operation, String type) {
    CapabilityStatementRestResourceOperationComponent capabilityOp = ConformanceHolder
        .getCapabilityResource(type).getOperation().stream().filter(op -> ("$" + op.getName()).equals(operation)).findFirst()
        .orElseThrow(() -> new FhirException(FhirestIssue.FEST_014, "operation", operation));
    OperationDefinition opDef = ConformanceHolder.getOperationDefinition(capabilityOp.getDefinition());
    if (opDef == null) {
      throw new FhirException(FhirestIssue.FEST_015, "operation", operation);
    }
    return opDef;
  }
}
