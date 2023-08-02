package com.kodality.kefhir.rest.filter;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import io.micronaut.http.MediaType;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

@Singleton
@RequiredArgsConstructor
public class FormatFilter implements KefhirRequestFilter {
  private static final String FORMAT = "_format";
  private final ResourceFormatService resourceFormatService;

  @Override
  public Integer getOrder() {
    return READ;
  }

  @Override
  public void handleRequest(KefhirRequest req) {
    String format = req.getParameter(FORMAT);
    if (format != null) {
      String mime = resourceFormatService.findPresenter(format).map(p -> p.getMimeTypes().get(0))
          .orElseThrow(() -> new FhirException(406, IssueType.INVALID, "unsupported _format"));
      req.setAccept(List.of(MediaType.of(mime)));
      req.setContentType(MediaType.of(mime));
      req.getParameters().remove(FORMAT);
    }
    if (req.getAccept() == null) {
      req.setAccept(req.getContentType() == null ? MediaType.ALL_TYPE : req.getContentType());
    }
    if (resourceFormatService.findSupported(req.getAccept().stream().map(MediaType::getName).toList()).isEmpty()) {
      throw new FhirException(406, IssueType.INVALID, "unsupported Accept");
    }
  }
}
