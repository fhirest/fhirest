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
 package com.kodality.kefhir.rest.exception;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.OperationOutcome;

import static java.util.stream.Collectors.joining;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class FhirExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<?>> {
  private final ResourceFormatService resourceFormatService;

  @Override
  public HttpResponse<?> handle(HttpRequest request, Throwable e) {
    Throwable root = ExceptionUtils.getRootCause(e);

    if (e instanceof FhirException) {
      return toResponse(request, (FhirException) e);
    }
    if (root instanceof FhirException) {
      return toResponse(request, (FhirException) root);
    }

    log.error("hello", e);
    return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
  }

  private HttpResponse toResponse(HttpRequest<?> request, FhirException e) {
    log(e);

    MutableHttpResponse<?> response = HttpResponse.status(HttpStatus.valueOf(e.getStatusCode()));
    if (e.getIssues() != null) {
      OperationOutcome outcome = new OperationOutcome();
      outcome.setExtension(e.getExtensions());
      outcome.setIssue(e.getIssues());
      outcome.setContained(e.getContained());
      String ct = request.getHeaders().get("Accept") == null ? "application/json" : request.getHeaders().get("Accept");
      ResourceContent c = resourceFormatService.compose(outcome, ct);
      response.body(c.getValue());
      response.contentType(c.getContentType());
    }
    return response;
  }

  private static void log(FhirException e) {
    String issues = e.getIssues().stream().map(i -> i.getDetails().getText()).collect(joining(", "));
    String msg = "hello " + e.getStatusCode() + " = " + issues;
    if (e.getStatusCode() >= 500) {
      log.error(msg);
    } else {
      log.info(msg);
    }
  }

}
