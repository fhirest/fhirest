package com.kodality.kefhir.rest.filter;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.structure.api.FhirContentType;
import javax.inject.Singleton;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

@Singleton
public class ContentTypeValidationFilter implements KefhirRequestFilter {

  @Override
  public Integer getOrder() {
    return VALIDATE;
  }

  @Override
  public void handleRequest(KefhirRequest req) {
    if (req.getAccept().size() > 0 && req.getAccept().stream().noneMatch(a -> "*/*".equals(a.getName()) || FhirContentType.getMediaTypes().contains(a.getName()))) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Accept");
    }
    if (req.getContentType() != null && !FhirContentType.getMediaTypes().contains(req.getContentType().getName())) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Content-Type");
    }
  }
}
