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
 package ee.tehik.fhirest.auth.http;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toList;

public class HttpAuthorization {
  private final String type;
  private final String credential;

  public HttpAuthorization() {
    this(null, null);
  }

  public HttpAuthorization(String type, String credential) {
    this.type = type;
    this.credential = credential;
  }

  public static List<HttpAuthorization> parse(Collection<String> headers) {
    if (CollectionUtils.isEmpty(headers)) {
      return Collections.emptyList();
    }
    return headers.stream()
        .filter(h -> !StringUtils.isEmpty(h))
        .flatMap(h -> Stream.of(h.split(",")))
        .map(a -> a.trim())
        .map(auth -> {
          String[] parts = auth.split("\\s");
          if (parts.length < 2) {
            return null;
          }
          return new HttpAuthorization(parts[0], parts[1]);
        })
        .filter(a -> a != null)
        .collect(toList());
  }

  public String getType() {
    return type;
  }

  public String getCredential() {
    return credential;
  }

  public boolean isType(String type) {
    return StringUtils.equals(type, this.type);
  }

  public String getCredentialDecoded() {
    try {
      return new String(Base64.getDecoder().decode(credential), "UTF8");
    } catch (IllegalArgumentException | UnsupportedEncodingException e) {
      return credential;
    }
  }
}
