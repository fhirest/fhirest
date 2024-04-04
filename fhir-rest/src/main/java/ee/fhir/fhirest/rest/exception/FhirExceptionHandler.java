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
package ee.fhir.fhirest.rest.exception;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.joining;

@Slf4j
@Component
@RequiredArgsConstructor
public class FhirExceptionHandler {
  private final ResourceFormatService resourceFormatService;

  public ResponseEntity<String> handle(HttpServletRequest request, Throwable e) {
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
    return toResponse(request, outcome, HttpStatus.INTERNAL_SERVER_ERROR.value());
  }


  private ResponseEntity<String> toResponse(HttpServletRequest request, FhirException e) {
    log(e);

    OperationOutcome outcome = new OperationOutcome();
    outcome.setExtension(e.getExtensions());
    outcome.setIssue(e.getIssues());
    outcome.setContained(e.getContained());
    return toResponse(request, outcome, e.getHttpCode());
  }

  protected ResponseEntity<String> toResponse(HttpServletRequest request, OperationOutcome outcome, int statusCode) {
    List<String> ct = request.getHeader("Accept") == null ? List.of() : List.of(request.getHeader("Accept").split(","));
    String accept = resourceFormatService.findSupported(ct).stream().findFirst().orElse("application/json");
    ResourceContent c = resourceFormatService.compose(outcome, accept);

    return ResponseEntity.status(HttpStatus.valueOf(statusCode))
        .header(HttpHeaders.CONTENT_TYPE, toHttpContentType(c.getContentType()))
        .body(c.getValue());
  }

  private static String toHttpContentType(String fhirContentType) {
    return fhirContentType.equals("json") ? "application/json" : fhirContentType;
  }

  private static void log(FhirException e) {
    String issues = e.getIssues().stream().map(i -> i.getDetails().getText()).collect(joining(", "));
    String msg = "Status " + e.getHttpCode() + " = " + issues;
    if (e.getHttpCode() >= 500) {
      log.error(msg);
    } else {
      log.info(msg);
    }
  }

}
