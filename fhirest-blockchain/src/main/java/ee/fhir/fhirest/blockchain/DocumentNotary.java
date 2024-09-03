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

package ee.fhir.fhirest.blockchain;

import ee.fhir.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.util.JsonUtil;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class DocumentNotary extends ResourceAfterSaveInterceptor {
  @Value("${gateway.endpoint}")
  private String gwEndpoint;
  private final HttpClient httpClient;

  @Inject
  private ResourceFormatService formatService;

  public DocumentNotary() {
    super(ResourceAfterSaveInterceptor.FINALIZATION);
    httpClient = java.net.http.HttpClient.newBuilder().build();
  }


  public String checkDocument(ResourceVersion version) {
    return post("/checkDocument", new DocumentPayload(version.getReference(), toJson(version))).body();
  }

  private HttpResponse<String> post(String path, Object payload) {
    HttpRequest req = HttpRequest.newBuilder(URI.create(gwEndpoint + path))
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(JsonUtil.toJson(payload)))
        .build();
    return httpClient.sendAsync(req, BodyHandlers.ofString()).join();
  }

  @Override
  public void handle(ResourceVersion version) {
    //TODO: queue
    CompletableFuture.runAsync(() -> {
      String hash = post("/notarize", new DocumentPayload(version.getReference(), toJson(version))).body();
      //TODO: check if hash needs to be stored in db
      //TODO: log success/fail
    });
  }

  private String toJson(ResourceVersion version) {
    Resource parse = formatService.parse(version.getContent().getValue());
    parse.setMeta(null);
    parse.setId((String) null); //TODO: change
    return formatService.compose(parse, "json").getValue();
  }
}
