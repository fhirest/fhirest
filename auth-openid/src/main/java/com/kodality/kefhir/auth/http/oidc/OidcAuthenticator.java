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
package com.kodality.kefhir.auth.http.oidc;

import com.kodality.kefhir.auth.User;
import com.kodality.kefhir.auth.http.AuthenticationProvider;
import com.kodality.kefhir.auth.http.HttpAuthorization;
import com.kodality.kefhir.rest.model.KefhirRequest;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;

//TODO: .well-known
@Singleton
@RequiredArgsConstructor
public class OidcAuthenticator implements AuthenticationProvider {
  private OidcUserProvider oidcUserProvider;

  @Override
  public User autheticate(KefhirRequest request) {
    List<HttpAuthorization> auths = HttpAuthorization.parse(request.getHeaders().get("Authorization"));
    String bearerToken = auths.stream().filter(a -> a.isType("Bearer")).findFirst().map(HttpAuthorization::getCredential).orElse(null);
    return oidcUserProvider.getUser(bearerToken);
  }

}
