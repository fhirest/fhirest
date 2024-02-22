package ee.fhir.fhirest.rest.filter;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FormatFilter implements FhirestRequestFilter {
  private static final String FORMAT = "_format";
  private final ResourceFormatService resourceFormatService;

  @Override
  public Integer getOrder() {
    return READ;
  }

  @Override
  public void handleRequest(FhirestRequest req) {
    String format = req.getParameter(FORMAT);
    if (format != null) {
      String mime = resourceFormatService.findPresenter(format).map(p -> p.getMimeTypes().get(0))
          .orElseThrow(() -> new FhirException(406, IssueType.INVALID, "unsupported _format"));
      req.setAccept(List.of(MediaType.valueOf(mime)));
      req.setContentType(MediaType.valueOf(mime));
      req.getParameters().remove(FORMAT);
    }
    if (req.getAccept() == null) {
      req.setAccept(MediaType.ALL);
    }
    if (req.getAccept().size() == 1 && req.getAccept().get(0).toString().equals(MediaType.ALL.toString()) && req.getContentType() != null) {
      req.setAccept(List.of(req.getContentType(), MediaType.ALL));
    }
    if (resourceFormatService.findSupported(req.getAccept().stream().map(MediaType::toString).toList()).isEmpty()) {
      throw new FhirException(406, IssueType.INVALID, "unsupported Accept");
    }
  }
}
