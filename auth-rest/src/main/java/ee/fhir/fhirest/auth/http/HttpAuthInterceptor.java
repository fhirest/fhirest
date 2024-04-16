/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
