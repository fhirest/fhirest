package com.kodality.kefhir.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.server.HttpServerConfiguration;
import io.micronaut.http.server.util.HttpHostResolver;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * don't forget to add nginx proxypass conf:
 * proxy_set_header Host $http_host/path;
 * proxy_set_header X-Forwarded-Host $http_host/path;
 * proxy_set_header X-Forwarded-Proto $scheme;
 */
@Singleton
@RequiredArgsConstructor
public class ServerUriHelper {

  private final HttpHostResolver httpHostResolver;
  private final HttpServerConfiguration httpServerConfiguration;

  public String buildServerUri(HttpRequest<?> request) {
    String contextPath = httpServerConfiguration.getContextPath();
    contextPath = contextPath != null ? StringUtils.prependIfMissing(contextPath, "/") : "";
    contextPath = StringUtils.removeEnd(contextPath, "/");
    return httpHostResolver.resolve(request) + contextPath;
  }

  public String getContextPath() {
    String contextPath = httpServerConfiguration.getContextPath();
    return contextPath != null ? StringUtils.removeStart(contextPath, "/") : "";
  }

  public String getHost(HttpRequest<?> request) {
    return httpHostResolver.resolve(request);
  }
}
