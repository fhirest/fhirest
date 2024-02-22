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
