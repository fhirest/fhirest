package ee.fhir.fhirest.rest.resource;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.rest.DefaultFhirResourceServer;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.rest.model.FhirestResponse;
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
    throw new FhirException(FhirestIssue.FEST_001, "desc", "'Binary' search not supported");
  }

  // TODO: should be saved differently
}
