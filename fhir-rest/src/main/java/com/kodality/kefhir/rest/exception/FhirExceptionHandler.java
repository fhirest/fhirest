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
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

import static java.util.stream.Collectors.joining;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class FhirExceptionHandler {
  private final ResourceFormatService resourceFormatService;

  public HttpResponse<?> handle(HttpRequest request, Throwable e) {
    Throwable root = ExceptionUtils.getRootCause(e);

    if (e instanceof FhirException) {
      return toResponse(request, (FhirException) e);
    }
    if (root instanceof FhirException) {
      return toResponse(request, (FhirException) root);
    }

    log.error("Fhir error occurred", e);
    OperationOutcome outcome = new OperationOutcome();
    outcome.addIssue().setCode(IssueType.EXCEPTION).setDetails(new CodeableConcept().setText(e.getMessage())).setSeverity(IssueSeverity.ERROR);
    return toResponse(request, outcome, HttpStatus.INTERNAL_SERVER_ERROR.getCode());
  }


  private HttpResponse toResponse(HttpRequest<?> request, FhirException e) {
    log(e);

    OperationOutcome outcome = new OperationOutcome();
    outcome.setExtension(e.getExtensions());
    outcome.setIssue(e.getIssues());
    outcome.setContained(e.getContained());
    return toResponse(request, outcome, e.getStatusCode());
  }

  protected MutableHttpResponse<?> toResponse(HttpRequest<?> request, OperationOutcome outcome, int statusCode) {
    String ct = request.getHeaders().get("Accept") == null ? "application/json" : request.getHeaders().get("Accept");
    ResourceContent c = resourceFormatService.compose(outcome, ct);

    MutableHttpResponse<?> response = HttpResponse.status(HttpStatus.valueOf(statusCode));
    response.body(c.getValue());
    String contentType = c.getContentType();
    response.contentType(toHttpContentType(contentType));
    return response;
  }

  private static String toHttpContentType(String fhirContentType) {
    return fhirContentType.equals("json") ? "application/json" : fhirContentType;
  }

  private static void log(FhirException e) {
    String issues = e.getIssues().stream().map(i -> i.getDetails().getText()).collect(joining(", "));
    String msg = "Status " + e.getStatusCode() + " = " + issues;
    if (e.getStatusCode() >= 500) {
      log.error(msg);
    } else {
      log.info(msg);
    }
  }

}
