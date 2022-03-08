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
package com.kodality.kefhir.rest;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.rest.filter.KefhirRequestFilter;
import com.kodality.kefhir.rest.filter.KefhirResponseFilter;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;
import com.kodality.kefhir.structure.api.FhirContentType;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Options;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Put;
import java.util.Comparator;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Resource;

@Consumes("*/*")
@Produces("*/*")
@Controller("/" + RuleThemAllFhirController.FHIR_ROOT)
@RequiredArgsConstructor
public class RuleThemAllFhirController {
  public static final String FHIR_ROOT = "fhir";
  private final KefhirEndpointService endpointService;
  private final ResourceFormatService resourceFormatService;
  private final List<KefhirRequestFilter> requestFilters;
  private final List<KefhirResponseFilter> responseFilters;

  @PostConstruct
  public void init() {
    requestFilters.sort(Comparator.comparing(KefhirRequestFilter::getOrder));
    responseFilters.sort(Comparator.comparing(KefhirResponseFilter::getOrder));
  }

  private HttpResponse<String> execute(HttpRequest request) {
    KefhirRequest req = buildKefhirRequest(request);
    req.setOperation(endpointService.findOperation(req));
    try {
      requestFilters.forEach(f -> f.handleRequest(req));
      KefhirResponse resp = endpointService.execute(req);
      responseFilters.forEach(f -> f.handleResponse(resp, req));
      return readKefhirResponse(resp, req);
    } catch (Throwable ex) {
      responseFilters.forEach(f -> f.handleException(ex, req));
      throw ex;
    }
  }

  private HttpResponse<String> readKefhirResponse(KefhirResponse resp, KefhirRequest req) {
    MutableHttpResponse<String> r = HttpResponse.status(HttpStatus.valueOf(resp.getStatus()));
    resp.getHeaders().forEach((k, vv) -> vv.forEach(v -> r.header(k, v)));
    if (resp.getBody() != null) {
      String accept = req.getHeader("Accept");
      ResourceContent formatted = format(resp.getBody(), accept);
      r.body(formatted.getValue());
      r.contentType(getResponseContentType(accept, formatted));
    }
    return r;
  }

  private String getResponseContentType(String accept, ResourceContent content) {
    List<String> contentTypes = resourceFormatService.findPresenter(content.getContentType()).get().getMimeTypes();
    if (accept != null && contentTypes.contains(accept)) {
      return accept;
    }
    return contentTypes.get(0);
  }

  private ResourceContent format(Object body, String contentType) {
    if (ResourceContent.class.isAssignableFrom(body.getClass())) {
      ResourceContent content = (ResourceContent) body;
      if (contentType == null || FhirContentType.isSameType(contentType, content.getContentType())) {
        return content;
      }
      Resource resource = resourceFormatService.parse(content.getValue());
      return resourceFormatService.compose(resource, contentType);
    }
    if (Resource.class.isAssignableFrom(body.getClass())) {
      Resource resource = (Resource) body;
      ResourceContent content = resourceFormatService.compose(resource, contentType);
      return content;
    }
    throw new FhirException(500, IssueType.PROCESSING, "cannot write " + body.getClass());
  }

  private KefhirRequest buildKefhirRequest(HttpRequest<String> request) {
    KefhirRequest req = new KefhirRequest();
    req.setMethod(request.getMethodName());
    String p = request.getPath();
    p = StringUtils.removeStart(p, "/");
    p = StringUtils.removeStart(p, FHIR_ROOT);
    p = StringUtils.removeStart(p, "/");
    req.setType(StringUtils.substringBefore(p, "/"));
    req.setPath(StringUtils.substringAfter(p, "/"));
    request.getHeaders().forEachValue((k, v) -> req.putHeader(k, v));
    request.getParameters().forEachValue((k, v) -> req.putParameter(k, v));
    req.setUri(req.getUri());
    req.setBody(request.getBody(String.class).orElse(null));
    return req;
  }

  @Get(uris = {"{path:.*}"})
  public HttpResponse<?> get(HttpRequest request) {
    return execute(request);
  }

  @Post(uris = {"{path:.*}"})
  public HttpResponse<?> post(HttpRequest request, @Body String json) {
    return execute(request);
  }

  @Put(uris = {"{path:.*}"})
  public HttpResponse<?> put(HttpRequest request, @Body String json) {
    return execute(request);
  }

  @Delete(uris = {"{path:.*}"})
  public HttpResponse<?> delete(HttpRequest request) {
    return execute(request);
  }

  @Options(uris = {"{path:.*}"})
  public HttpResponse<?> options(HttpRequest request) {
    return execute(request);
  }

}
