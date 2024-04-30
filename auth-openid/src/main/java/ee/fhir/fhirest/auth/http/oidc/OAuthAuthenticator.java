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
