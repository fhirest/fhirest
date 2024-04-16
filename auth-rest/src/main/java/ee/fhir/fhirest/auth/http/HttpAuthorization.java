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

import ee.fhir.fhirest.rest.model.FhirestRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
public class HttpAuthorization {
  public static final String AUTHORIZATION = "Authorization";
  public static final String BEARER = "Bearer";
  public static final String BASIC = "Basic";

  private final String type;
  private final String credential;

  public HttpAuthorization() {
    this(null, null);
  }

  public HttpAuthorization(String type, String credential) {
    this.type = type;
    this.credential = credential;
  }

  public static List<HttpAuthorization> readAuthorizations(FhirestRequest request) {
    return parse(request.getHeaders().get(AUTHORIZATION));
  }

  public static List<HttpAuthorization> parse(Collection<String> headers) {
    if (CollectionUtils.isEmpty(headers)) {
      return Collections.emptyList();
    }
    return headers.stream()
        .filter(h -> !StringUtils.isEmpty(h))
        .flatMap(h -> Stream.of(h.split(",")))
        .map(String::trim)
        .map(auth -> {
          String[] parts = auth.split("\\s");
          if (parts.length < 2) {
            return null;
          }
          return new HttpAuthorization(parts[0], parts[1]);
        })
        .filter(Objects::nonNull)
        .toList();
  }

  public boolean isType(String type) {
    return StringUtils.equals(type, this.type);
  }

  public String getCredentialDecoded() {
    try {
      return new String(Base64.getDecoder().decode(credential), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      return credential;
    }
  }
}
