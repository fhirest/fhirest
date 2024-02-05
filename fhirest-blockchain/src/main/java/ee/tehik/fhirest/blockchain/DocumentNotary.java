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
package ee.tehik.fhirest.blockchain;

import ee.tehik.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.tehik.fhirest.core.model.ResourceVersion;
import ee.tehik.fhirest.core.util.JsonUtil;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import io.micronaut.context.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hl7.fhir.r5.model.Resource;

@Singleton
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
