package ee.tehik.fhirest.auth.http.oidc;

import com.google.gson.Gson;
import ee.tehik.fhirest.auth.User;
import ee.tehik.fhirest.core.exception.FhirException;
import io.micronaut.context.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

import static java.util.stream.Collectors.toSet;

@Singleton
public class OidcUserProvider {
  private final HttpClient httpClient;
  @Value("${oidc.url}")
  private String oidcUrl;

  public OidcUserProvider() {
    httpClient = java.net.http.HttpClient.newBuilder().build();
  }

  public User getUser(String bearerToken) {
    if (bearerToken == null) {
      return null;
    }
    Map<String, Object> userJson = readProfile(bearerToken);
    if (userJson == null) {
      return null;
    }

    User user = new User();
    user.setScopes(getScopes(userJson));

    Map<String, Object> claims = new HashMap<>(userJson);
    claims.remove("auth_time");
    claims.remove("scope");
    user.setClaims(claims);
    return user;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Set<String> getScopes(Map<String, Object> userJson) {
    Object scope = userJson.get("scope");
    if (scope instanceof String) {
      return Stream.of(StringUtils.split((String) scope, ";")).map(String::trim).collect(toSet());
    }
    if (scope instanceof List) {
      return new HashSet<>((List) scope);
    }
    return null;
  }

  private Map<String, Object> readProfile(String bearer) {
    if (StringUtils.isEmpty(oidcUrl)) {
      throw new FhirException(500, IssueType.SECURITY, "server oidc config missing");
    }
    HttpRequest req = HttpRequest.newBuilder(URI.create(oidcUrl + "/userinfo")).header("Authorization", "Bearer " + bearer).GET().build();
    HttpResponse<String> response = httpClient.sendAsync(req, BodyHandlers.ofString()).join();
    if (response.statusCode() >= 400) {
      return null;
    }
    return fromJson(response.body());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> fromJson(String json) {
    return new Gson().fromJson(json, Map.class);
  }
}
