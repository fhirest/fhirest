/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
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

package ee.fhir.fhirest.rest.model;

import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.util.ResourceUtil;
import ee.fhir.fhirest.rest.FhirestEndpointService.FhirestEnabledOperation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class FhirestRequest {
  private String method;
  private String type;
  private String path;
  private Map<String, List<String>> parameters = new LinkedHashMap<>();
  private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private String body;
  private List<MediaType> accept;
  private MediaType contentType;

  private String transactionMethod;
  private FhirestEnabledOperation operation;
  /**
   * Use this for custom and internal logic
   */
  private Map<String, Object> properties = new LinkedHashMap<>();

  public void setAccept(MediaType accept) {
    this.accept = List.of(accept);
  }

  public void setAccept(List<MediaType> accept) {
    this.accept = accept;
  }

  public String getContentTypeName() {
    return contentType == null ? null : contentType.toString();
  }

  public void setPath(String path) {
    this.path = StringUtils.removeEnd(StringUtils.removeStart(path, "/"), "/");
  }

  public VersionId getReference() {
    return ResourceUtil.parseReference(type + "/" + getPath());
  }

  public String getHeader(String name) {
    return headers.containsKey(name) && !headers.get(name).isEmpty() ? headers.get(name).get(0) : null;
  }

  public void putHeader(String name, String value) {
    if (name == null || value == null) {
      return;
    }
    if (name.equalsIgnoreCase("Accept")) {
      setAccept(MediaType.parseMediaTypes(value));
      return;
    }
    if (name.equalsIgnoreCase("Content-Type")) {
      setContentType(MediaType.valueOf(value));
      return;
    }
    headers.computeIfAbsent(name, x -> new ArrayList<>()).add(value);
  }

  public String getParameter(String name) {
    return parameters.containsKey(name) && !parameters.get(name).isEmpty() ? parameters.get(name).get(0) : null;
  }

  public String getParametersString() {
    return parameters.entrySet().stream().flatMap(e -> e.getValue().stream().map(v -> e.getKey() + "=" + v)).collect(Collectors.joining("&"));
  }

  public String getFhirUrlString() {
    String url = (getType() == null ? "" : "/" + getType()) +
                 (StringUtils.isEmpty(getPath()) ? "" : "/" + getPath()) +
                 (getParameters().isEmpty() ? "" : "?" + getParametersString());
    return StringUtils.isEmpty(url) ? "/" : url;
  }

  public void putParameter(String name, String value) {
    if (name != null && value != null) {
      parameters.computeIfAbsent(name, x -> new ArrayList<>()).add(value);
    }
  }

  public void putQuery(String query) {
    if (query == null) {
      return;
    }
    Arrays.stream(query.split("&")).forEach(q -> {
      String[] p = q.split("=");
      putParameter(p[0], p[1]);
    });
  }
}
