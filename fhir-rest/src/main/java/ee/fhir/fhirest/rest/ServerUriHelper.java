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

package ee.fhir.fhirest.rest;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * don't forget to add nginx proxypass conf:
 * proxy_set_header Host $http_host/path;
 * proxy_set_header X-Forwarded-Host $http_host/path;
 * proxy_set_header X-Forwarded-Proto $scheme;
 */
@Component
@RequiredArgsConstructor
public class ServerUriHelper {
  private final ServletContext servletContext;
  private final HttpServletRequest request;

  /**
   * @return schema://host:port/context-path
   */
  public String getServerUri() {
    String contextPath = getContextPath();
    return StringUtils.removeEnd(getServerHost() + "/" + contextPath, "/");
  }

  /**
   * @return schema://host:port
   */
  public String getServerHost() {
    return createHost(request.getScheme(), request.getServerName(), request.getServerPort());
  }

  /**
   * @return configured spring server context path
   */
  public String getContextPath() {
    String contextPath = StringUtils.defaultString(servletContext.getContextPath());
    return StringUtils.strip(contextPath, "/");
  }


  private String createHost(String scheme, String host, Integer port) {
    scheme = scheme == null ? "http" : scheme;
    host = host == null ? "localhost" : host;
    if (port != null && port != 80 && port != 443) {
      return scheme + "://" + host + ":" + port;
    }
    return scheme + "://" + host;
  }


}
