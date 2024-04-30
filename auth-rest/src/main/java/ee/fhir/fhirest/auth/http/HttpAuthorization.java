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
