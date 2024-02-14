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
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(path = "/" + RuleThemAllFhirController.FHIR_ROOT, consumes = "*/*", produces = "*/*")
@Controller
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

  @RequestMapping(path = "{*path}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<String> get(HttpServletRequest request) {
    return execute(request);
  }

  private ResponseEntity<String> execute(HttpServletRequest request) {
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

  private ResponseEntity<String> readFhirestResponse(FhirestResponse resp, FhirestRequest req) {
    BodyBuilder r = ResponseEntity.status(HttpStatus.valueOf(resp.getStatus()));
    resp.getHeaders().forEach((k, vv) -> vv.stream().filter(Objects::nonNull).forEach(v -> r.header(k, v)));
    if (resp.getBody() == null) {
      return r.build();
    }
    List<String> accepts = req.getAccept().stream().map(MediaType::toString).toList();
    String accept = resourceFormatService.findSupported(accepts).get(0);
    ResourceContent formatted = format(resp.getBody(), accept);
    if ("true".equals(req.getParameter(PRETTY))) {
      prettify(formatted);
    }
    return r.header(HttpHeaders.CONTENT_TYPE, accept + ";charset=utf-8")
        .body(formatted.getValue());
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

  private FhirestRequest buildFhirestRequest(HttpServletRequest request) {
    FhirestRequest req = new FhirestRequest();
    req.setMethod(request.getMethod());
    String p = StringUtils.defaultString(request.getRequestURI());
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
    Collections.list(request.getHeaderNames()).forEach(n -> req.putHeader(n, request.getHeader(n)));
    request.getParameterMap().forEach((k, vv) -> Stream.of(vv).forEach(v -> req.putParameter(k, v)));
    req.setBody(readBody(request));
    return req;
  }

  private String readBody(HttpServletRequest req) {
    try {
      ServletInputStream is = req.getInputStream();
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      for (int length; (length = is.read(buffer)) != -1; ) {
        result.write(buffer, 0, length);
      }
      return result.toString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
