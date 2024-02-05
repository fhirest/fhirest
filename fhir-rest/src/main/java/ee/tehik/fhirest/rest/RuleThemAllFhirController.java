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
package ee.tehik.fhirest.rest;

import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.rest.exception.FhirExceptionHandler;
import ee.tehik.fhirest.rest.filter.FhirestRequestFilter;
import ee.tehik.fhirest.rest.filter.FhirestResponseFilter;
import ee.tehik.fhirest.rest.model.FhirestRequest;
import ee.tehik.fhirest.rest.model.FhirestResponse;
import ee.tehik.fhirest.structure.api.ResourceContent;
import ee.tehik.fhirest.structure.service.ContentTypeService;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Put;
import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Resource;

@Consumes("*/*")
@Produces("*/*")
@Controller("/" + RuleThemAllFhirController.FHIR_ROOT)
@RequiredArgsConstructor
public class RuleThemAllFhirController {
  private static final String PRETTY = "_pretty";
  public static final String FHIR_ROOT = "fhir";
  private final FhirestEndpointService endpointService;
  private final ResourceFormatService resourceFormatService;
  private final ContentTypeService contentTypeService;
  private final List<FhirestRequestFilter> requestFilters;
  private final List<FhirestResponseFilter> responseFilters;
  private final FhirExceptionHandler fhirExceptionHandler;
  private final ServerUriHelper serverUriHelper;

  @PostConstruct
  public void init() {
    requestFilters.sort(Comparator.comparing(FhirestRequestFilter::getOrder));
    responseFilters.sort(Comparator.comparing(FhirestResponseFilter::getOrder));
  }

  private HttpResponse<?> execute(HttpRequest request) {
    try {
      FhirestRequest req = buildFhirestRequest(request);
      req.setOperation(endpointService.findOperation(req));
      try {
        requestFilters.forEach(f -> f.handleRequest(req));
        FhirestResponse resp = endpointService.execute(req);
        responseFilters.forEach(f -> f.handleResponse(resp, req));
        return readFhirestResponse(resp, req);
      } catch (Exception ex) {
        responseFilters.forEach(f -> f.handleException(ex, req));
        throw ex;
      }
    } catch (Exception ex) {
      return fhirExceptionHandler.handle(request, ex);
    }
  }

  private HttpResponse<String> readFhirestResponse(FhirestResponse resp, FhirestRequest req) {
    MutableHttpResponse<String> r = HttpResponse.status(HttpStatus.valueOf(resp.getStatus()));
    resp.getHeaders().forEach((k, vv) -> vv.stream().filter(Objects::nonNull).forEach(v -> r.header(k, v)));
    if (resp.getBody() != null) {
      List<String> accepts = req.getAccept().stream().map(MediaType::getName).toList();
      String accept = resourceFormatService.findSupported(accepts).get(0);
      ResourceContent formatted = format(resp.getBody(), accept);
      if ("true".equals(req.getParameter(PRETTY))) {
        prettify(formatted);
      }
      r.body(formatted.getValue());
      r.contentType(accept + ";charset=utf-8");
    }
    return r;
  }

  private ResourceContent format(Object body, String contentType) {
    if (ResourceContent.class.isAssignableFrom(body.getClass())) {
      ResourceContent content = (ResourceContent) body;
      if (contentType == null || contentTypeService.isSameType(contentType, content.getContentType())) {
        return content;
      }
      Resource resource = resourceFormatService.parse(content.getValue());
      return resourceFormatService.compose(resource, contentType);
    }
    if (Resource.class.isAssignableFrom(body.getClass())) {
      Resource resource = (Resource) body;
      return resourceFormatService.compose(resource, contentType);
    }
    throw new FhirException(500, IssueType.PROCESSING, "cannot write " + body.getClass());
  }

  private void prettify(ResourceContent content) {
    content.setValue(resourceFormatService.findPresenter(content.getContentType()).get().prettify(content.getValue()));
  }

  private FhirestRequest buildFhirestRequest(HttpRequest<String> request) {
    FhirestRequest req = new FhirestRequest();
    req.setServerUri(serverUriHelper.buildServerUri(request));
    req.setServerHost(serverUriHelper.getHost(request));
    req.setMethod(request.getMethodName());
    String p = request.getPath();
    p = StringUtils.removeStart(p, "/");
    p = StringUtils.removeStart(p, serverUriHelper.getContextPath());
    p = StringUtils.removeStart(p, "/");
    p = StringUtils.removeStart(p, FHIR_ROOT);
    p = StringUtils.removeStart(p, "/");
    if (!p.isEmpty() && Character.isUpperCase(p.charAt(0))) { // a bit ugly way to understand is it a /root request or not
      req.setType(StringUtils.substringBefore(p, "/"));
      req.setPath(StringUtils.substringAfter(p, "/"));
    } else {
      req.setPath(p);
    }
    request.getHeaders().forEachValue(req::putHeader);
    request.getParameters().forEachValue(req::putParameter);
    req.setUri(req.getUri());
    req.setBody(request.getBody(String.class).orElse(null));
    return req;
  }

  @Get(uris = {"{path:.*}"})
  public HttpResponse<?> get(HttpRequest request) {
    return execute(request);
  }

  @Post(uris = {"{path:.*}"})
  public HttpResponse<?> post(HttpRequest request, @Nullable @Body String json) {
    return execute(request);
  }

  @Put(uris = {"{path:.*}"})
  public HttpResponse<?> put(HttpRequest request, @Nullable @Body String json) {
    return execute(request);
  }

  @Delete(uris = {"{path:.*}"})
  public HttpResponse<?> delete(HttpRequest request) {
    return execute(request);
  }

}
