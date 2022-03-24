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
    String accept = req.getHeader("Accept");
    if(accept.contains(";charset=")){
      accept = accept.replaceAll(";charset=[^;]*", "");
    }
    if (accept != null && !"*/*".equals(accept) && !FhirContentType.getMediaTypes().contains(accept)) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Accept");
    }
    String contentType = req.getHeader("Content-Type");
    if (contentType != null && !FhirContentType.getMediaTypes().contains(contentType)) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Content-Type");
    }
  }
}
