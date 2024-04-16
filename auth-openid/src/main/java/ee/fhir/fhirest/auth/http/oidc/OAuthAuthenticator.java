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
package ee.fhir.fhirest.auth.http.oidc;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import ee.fhir.fhirest.auth.User;
import ee.fhir.fhirest.auth.http.AuthenticationProvider;
import ee.fhir.fhirest.auth.http.HttpAuthorization;
import ee.fhir.fhirest.core.service.cache.FhirestCache;
import ee.fhir.fhirest.core.service.cache.FhirestCacheManager;
import ee.fhir.fhirest.core.util.JsonUtil;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
public class OAuthAuthenticator implements AuthenticationProvider {
  private final HttpClient httpClient;
  private final String jwksUrl;
  private final FhirestCache jwksCache;

  public OAuthAuthenticator(@Value("${fhirest.oauth.jwks-url}") String jwksUrl, FhirestCacheManager cacheManager) {
    this.jwksUrl = jwksUrl;
    this.jwksCache = cacheManager.registerCache("jwks", 1, 60 * 60);
    this.httpClient = java.net.http.HttpClient.newBuilder().build();
  }

  @Override
  public User autheticate(FhirestRequest request) {
    return HttpAuthorization.readAuthorizations(request).stream()
        .filter(a -> a.isType(HttpAuthorization.BEARER))
        .map(bearer -> getUser(bearer.getCredential()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private User getUser(String token) {
    try {
      DecodedJWT jwt = JWT.decode(token);
      Jwk jwk = getJwks().get(jwt.getKeyId());
      if (jwk == null) {
        return null;
      }
      Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null).verify(jwt);

      if (jwt.getExpiresAt().before(new Date())) {
        return null;
      }
      Map<String, Object> claims = JsonUtil.fromJson(new String(Base64.getUrlDecoder().decode(jwt.getPayload())));
      User user = new User();
      user.setName((String) claims.get("sub"));
      user.setScopes(getScopes(claims));
      user.setClaims(claims);
      return user;
    } catch (SignatureVerificationException | JwkException | JWTDecodeException e) {
      log.debug("bearer token verification", e);
      return null;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Set<String> getScopes(Map<String, Object> claims) {
    Object scope = claims.get("scope");
    if (scope instanceof String) {
      return Stream.of(StringUtils.split((String) scope, ";")).map(String::trim).collect(toSet());
    }
    if (scope instanceof List) {
      return new HashSet<>((List) scope);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Jwk> getJwks() {
    return jwksCache.getCf("", () -> httpClient.sendAsync(HttpRequest.newBuilder().GET().uri(URI.create(jwksUrl)).build(), BodyHandlers.ofString())
        .thenApply(resp -> {
          Map<String, Object> body = JsonUtil.fromJson(resp.body());
          List<Map<String, Object>> keys = (List<Map<String, Object>>) body.get("keys");
          return keys.stream().map(Jwk::fromValues).collect(Collectors.toMap(Jwk::getId, k -> k));
        })).join();
  }


}
