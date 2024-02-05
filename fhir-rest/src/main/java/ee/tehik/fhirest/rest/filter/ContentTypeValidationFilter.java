package ee.tehik.fhirest.rest.filter;

import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.rest.model.FhirestRequest;
import ee.tehik.fhirest.structure.service.ContentTypeService;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

@Singleton
@RequiredArgsConstructor
public class ContentTypeValidationFilter implements FhirestRequestFilter {
  private final ContentTypeService contentTypeService;

  @Override
  public Integer getOrder() {
    return VALIDATE;
  }

  @Override
  public void handleRequest(FhirestRequest req) {
    if (req.getAccept().size() > 0 &&
        req.getAccept().stream().noneMatch(a -> "*/*".equals(a.getName()) || contentTypeService.getMediaTypes().contains(a.getName()))) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Accept");
    }
    if (req.getContentType() != null && !contentTypeService.getMediaTypes().contains(req.getContentType().getName())) {
      throw new FhirException(406, IssueType.NOTSUPPORTED, "invalid Content-Type");
    }
  }
}
