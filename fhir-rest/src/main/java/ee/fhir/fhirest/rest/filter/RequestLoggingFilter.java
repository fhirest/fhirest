package ee.fhir.fhirest.rest.filter;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.rest.model.FhirestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static ee.fhir.fhirest.rest.model.FhirestRequestProperties.USERNAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter implements FhirestRequestFilter, FhirestResponseFilter {
  public static final String LOG_REQ_START = "log_req_start";

  @Override
  public Integer getOrder() {
    return 10;
  }

  @Override
  public void handleRequest(FhirestRequest req) {
    req.getProperties().put(LOG_REQ_START, System.currentTimeMillis());
  }

  @Override
  public void handleResponse(FhirestResponse response, FhirestRequest request) {
    Long start = (Long) request.getProperties().get(LOG_REQ_START);
    long now = System.currentTimeMillis();
    String username = StringUtils.defaultIfEmpty((String) request.getProperties().get(USERNAME), "-");
    log.info(String.format("FHIR_REQ %s %s %s %s %sms", username, request.getMethod(), request.getFhirUrlString(), response.getStatus(), now - start));
  }

  @Override
  public void handleException(Throwable ex, FhirestRequest request) {
    Long start = (Long) request.getProperties().get(LOG_REQ_START);
    long now = System.currentTimeMillis();
    String username = StringUtils.defaultIfEmpty((String) request.getProperties().get(USERNAME), "-");
    if (ex instanceof FhirException fe) {
      log.info(String.format("FHIR_REQ_ERROR %s %s %s %s %sms", username, request.getMethod(), request.getFhirUrlString(), fe.getHttpCode(), now - start));
      return;
    }
    log.error(String.format("FHIR_REQ_EXCEPTION %s %s %s %sms", username, request.getMethod(), request.getFhirUrlString(), now - start));
  }

}
