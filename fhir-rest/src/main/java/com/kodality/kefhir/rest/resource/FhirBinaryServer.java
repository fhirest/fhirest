package com.kodality.kefhir.rest.resource;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.rest.DefaultFhirResourceServer;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;
import jakarta.inject.Singleton;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.ResourceType;

@Singleton
public class FhirBinaryServer extends DefaultFhirResourceServer {

  @Override
  public String getTargetType() {
    return ResourceType.Binary.name();
  }

  @Override
  public KefhirResponse search(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "'Binary' search not supported");
  }

  // TODO: should be saved differently

}
