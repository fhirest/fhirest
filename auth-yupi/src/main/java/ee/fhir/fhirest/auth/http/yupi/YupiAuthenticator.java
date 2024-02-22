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
package ee.fhir.fhirest.auth.http.yupi;

import ee.fhir.fhirest.auth.User;
import ee.fhir.fhirest.auth.http.AuthenticationProvider;
import ee.fhir.fhirest.auth.http.HttpAuthorization;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class YupiAuthenticator implements AuthenticationProvider {
  private static final Map<String, String> yupiOrgs = Map.of(
      "yupi", "yupland",
      "ipuy", "dnalpuy"
  );

  @Override
  public User autheticate(FhirestRequest request) {
    List<HttpAuthorization> auths = HttpAuthorization.parse(request.getHeaders().get("Authorization"));
    return auths.stream()
        .filter(a -> a.isType("Bearer"))
        .filter(bearer -> yupiOrgs.containsKey(bearer.getCredential()))
        .map(bearer -> makeYupi(bearer.getCredential(), yupiOrgs.get(bearer.getCredential())))
        .map(user -> decorateClaims(user, getClaimHeaders(request))).findFirst().orElse(null);
  }

  private Map<String, String> getClaimHeaders(FhirestRequest request) {
    Map<String, String> claims = new HashMap<>();
    request.getHeaders().keySet().stream().filter(k -> k.startsWith("x-claim-")).forEach(k -> {
      String claim = k.replace("x-claim-", "");
      claims.put(claim, request.getHeader(k));
    });
    return claims;
  }

  private User decorateClaims(User user, Map<String, String> claimHeaders) {
    claimHeaders.forEach((k, v) -> user.getClaims().put(k, v));
    return user;
  }

  private static User makeYupi(String sub, String org) {
    User user = new User();
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", sub);
    claims.put("org", org);
    user.setClaims(claims);
    user.setScopes(Collections.singleton("user/*.*"));
    return user;
  }

}
