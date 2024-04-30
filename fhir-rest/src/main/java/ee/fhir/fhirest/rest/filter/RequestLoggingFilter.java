/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
