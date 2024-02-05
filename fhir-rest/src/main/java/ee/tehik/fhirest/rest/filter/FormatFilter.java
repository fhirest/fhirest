package ee.tehik.fhirest.rest.filter;

import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.rest.model.FhirestRequest;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import io.micronaut.http.MediaType;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

@Singleton
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
      req.setAccept(List.of(MediaType.of(mime)));
      req.setContentType(MediaType.of(mime));
      req.getParameters().remove(FORMAT);
    }
    if (req.getAccept() == null) {
      req.setAccept(MediaType.ALL_TYPE);
    }
    if (req.getAccept().size() == 1 && req.getAccept().get(0).getName().equals(MediaType.ALL_TYPE.getName()) && req.getContentType() != null) {
      req.setAccept(List.of(req.getContentType(), MediaType.ALL_TYPE));
    }
    if (resourceFormatService.findSupported(req.getAccept().stream().map(MediaType::getName).toList()).isEmpty()) {
      throw new FhirException(406, IssueType.INVALID, "unsupported Accept");
    }
  }
}
