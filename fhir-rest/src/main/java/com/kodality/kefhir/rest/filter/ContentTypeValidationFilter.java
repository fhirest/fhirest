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
    String accept = clean(req.getHeader("Accept"));
    if (accept != null && !"*/*".equals(accept) && !FhirContentType.getMediaTypes().contains(accept)) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Accept");
    }
    String contentType = clean(req.getHeader("Content-Type"));
    if (contentType != null && !FhirContentType.getMediaTypes().contains(contentType)) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Content-Type");
    }
  }

  private String clean(String ct) {
    if (ct.contains(";charset=")) {
      return ct.replaceAll(";charset=[^;]*", "");
    }
    return ct;
  }
}
