package ee.tehik.fhirest.rest.resource;

import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.rest.DefaultFhirResourceServer;
import ee.tehik.fhirest.rest.model.FhirestRequest;
import ee.tehik.fhirest.rest.model.FhirestResponse;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.ResourceType;
import org.springframework.stereotype.Component;

@Component
public class FhirBinaryServer extends DefaultFhirResourceServer {

  @Override
  public String getTargetType() {
    return ResourceType.Binary.name();
  }

  @Override
  public FhirestResponse search(FhirestRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "'Binary' search not supported");
  }

  // TODO: should be saved differently

}
