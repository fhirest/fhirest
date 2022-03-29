package com.kodality.kefhir.rest.filter;

import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import io.micronaut.http.MediaType;
import java.util.List;
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
      req.setAccept(List.of(MediaType.of(mime)));
      req.setContentType(MediaType.of(mime));
      req.getParameters().remove(FORMAT);
    }
    if (req.getAccept() == null || req.getAccept().get(0).getName().equals("*/*") || req.getAccept().get(0).getName().equals("text/html")) { // XXX text/html?
      req.setAccept(req.getContentType() == null ? MediaType.APPLICATION_JSON_TYPE : req.getContentType());
    }
  }
}
