package com.kodality.kefhir.rest.filter;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.structure.service.ContentTypeService;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

@Singleton
@RequiredArgsConstructor
public class ContentTypeValidationFilter implements KefhirRequestFilter {
  private final ContentTypeService contentTypeService;

  @Override
  public Integer getOrder() {
    return VALIDATE;
  }

  @Override
  public void handleRequest(KefhirRequest req) {
    if (req.getAccept().size() > 0 &&
        req.getAccept().stream().noneMatch(a -> "*/*".equals(a.getName()) || contentTypeService.getMediaTypes().contains(a.getName()))) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Accept");
    }
    if (req.getContentType() != null && !contentTypeService.getMediaTypes().contains(req.getContentType().getName())) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Content-Type");
    }
  }
}
