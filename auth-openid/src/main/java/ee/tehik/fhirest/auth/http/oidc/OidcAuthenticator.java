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
package ee.tehik.fhirest.auth.http.oidc;

import ee.tehik.fhirest.auth.User;
import ee.tehik.fhirest.auth.http.AuthenticationProvider;
import ee.tehik.fhirest.auth.http.HttpAuthorization;
import ee.tehik.fhirest.rest.model.FhirestRequest;
import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

//TODO: .well-known
@Component
@RequiredArgsConstructor
public class OidcAuthenticator implements AuthenticationProvider {
  private OidcUserProvider oidcUserProvider;

  @Override
  public User autheticate(FhirestRequest request) {
    List<HttpAuthorization> auths = HttpAuthorization.parse(request.getHeaders().get("Authorization"));
    String bearerToken = auths.stream().filter(a -> a.isType("Bearer")).findFirst().map(HttpAuthorization::getCredential).orElse(null);
    return oidcUserProvider.getUser(bearerToken);
  }

}
