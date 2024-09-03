/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
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

package ee.fhir.fhirest.auth.http;

import ee.fhir.fhirest.auth.ClientIdentity;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.rest.filter.FhirestRequestFilter;
import ee.fhir.fhirest.rest.filter.FhirestResponseFilter;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.rest.model.FhirestResponse;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.springframework.stereotype.Component;

import static ee.fhir.fhirest.rest.model.FhirestRequestProperties.USERNAME;

@RequiredArgsConstructor
@Component
@Slf4j
public class HttpAuthInterceptor implements FhirestRequestFilter, FhirestResponseFilter {
  private final ClientIdentity clientIdentity;
  private final List<AuthenticationProvider> authenticators;

  @Override
  public Integer getOrder() {
    return FhirestRequestFilter.VALIDATE - 1;
  }

  @Override
  public void handleResponse(FhirestResponse response, FhirestRequest request) {
    clientIdentity.remove();
  }

  @Override
  public void handleException(Throwable ex, FhirestRequest request) {
    clientIdentity.remove();
  }

  @Override
  public void handleRequest(FhirestRequest request) {
    if ("metadata".equals(request.getPath())) {
      return;
    }

    if (clientIdentity.isAuthenticated()) {
      throw new IllegalStateException("context cleanup not worked, panic");
    }

    authenticators.stream().map(a -> a.autheticate(request)).filter(Objects::nonNull).findFirst().ifPresent(user -> {
      clientIdentity.set(user);
      request.getProperties().put(USERNAME, user.getName());
    });

    if (!clientIdentity.isAuthenticated()) {
      log.debug("could not authenticate. tried services: " + authenticators);
      throw new FhirException(401, IssueType.LOGIN, "not authenticated");
    }

  }

}
