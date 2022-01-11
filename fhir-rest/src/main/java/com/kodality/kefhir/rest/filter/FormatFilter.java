package com.kodality.kefhir.rest.filter;

import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;

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
      String mime = resourceFormatService.findPresenter(format).get().getMimeTypes().get(0);
      req.setHeader("Accept", mime);
      req.setHeader("Content-Type", mime);
      req.getParameters().remove(FORMAT);
    }
    if (req.getHeader("Accept") == null || req.getHeader("Accept").equals("*/*")) {
      req.setHeader("Accept", req.getHeader("Content-Type") == null ? "application/json" : req.getHeader("Content-Type"));
    }
  }
}
