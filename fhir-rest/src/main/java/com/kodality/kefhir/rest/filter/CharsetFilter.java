package com.kodality.kefhir.rest.filter;

import com.kodality.kefhir.rest.model.KefhirRequest;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class CharsetFilter implements KefhirRequestFilter {

  @Override
  public Integer getOrder() {
    return RECEIVE;
  }

  @Override
  public void handleRequest(KefhirRequest req) {
    if (req.getHeader("Accept") != null) {
      req.getHeaders().put("Accept", List.of(clean(req.getHeader("Accept"))));
    }
    if (req.getHeader("Content-Type") != null) {
      req.getHeaders().put("Content-Type", List.of(clean(req.getHeader("Content-Type"))));
    }
  }

  private String clean(String ct) {
    if (ct != null && ct.contains(";charset=")) {
      return ct.replaceAll(";charset=[^;]*", "");
    }
    return ct;
  }
}
