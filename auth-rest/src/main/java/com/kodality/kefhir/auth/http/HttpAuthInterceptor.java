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
package com.kodality.kefhir.auth.http;

import com.kodality.kefhir.auth.ClientIdentity;
import com.kodality.kefhir.auth.User;
import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.rest.filter.KefhirRequestFilter;
import com.kodality.kefhir.rest.filter.KefhirResponseFilter;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;
import java.util.List;
import java.util.Objects;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class HttpAuthInterceptor implements KefhirRequestFilter, KefhirResponseFilter {
  private final ClientIdentity clientIdentity;
  private final List<AuthenticationProvider> authenticators;

  @Override
  public Integer getOrder() {
    return KefhirRequestFilter.VALIDATE - 1;
  }

  @Override
  public void handleResponse(KefhirResponse response, KefhirRequest request) {
    clientIdentity.remove();
  }

  @Override
  public void handleException(Throwable ex, KefhirRequest request) {
    clientIdentity.remove();
  }

  @Override
  public void handleRequest(KefhirRequest request) {
    if ("metadata".equals(request.getPath())) {
      return;
    }

    if (clientIdentity.isAuthenticated()) {
      throw new IllegalStateException("context cleanup not worked, panic");
    }

    User user = authenticators.stream().map(a -> a.autheticate(request)).filter(Objects::nonNull).findFirst().orElse(null);
    clientIdentity.set(user);

    if (!clientIdentity.isAuthenticated()) {
      log.debug("could not authenticate. tried services: " + authenticators);
      throw new FhirException(401, IssueType.LOGIN, "not authenticated");
    }

  }

}
