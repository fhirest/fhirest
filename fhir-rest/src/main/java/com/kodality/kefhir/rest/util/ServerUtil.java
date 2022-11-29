package com.kodality.kefhir.rest.util;

import io.micronaut.http.HttpRequest;
import java.net.InetSocketAddress;

public class ServerUtil {
  public static String getServerUri(HttpRequest<?> request) {
    String proto = request.getHeaders().get("X-Forwarded-Proto", String.class, "http");
    InetSocketAddress serverAddress = request.getServerAddress();
    int port = serverAddress.getPort();
    String host = request.getHeaders().get("Host", String.class, "localhost:" + port);
    return proto + "://" + host;
  }
}
