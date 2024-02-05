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
 package ee.tehik.fhirest.core.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StringType;

import static java.util.stream.Collectors.joining;

public class FhirException extends RuntimeException {
  private final int httpCode;

  private final List<OperationOutcomeIssueComponent> issues;
  private List<Extension> extensions;
  private List<Resource> contained;

  public FhirException(int httpCode, IssueType type, String description) {
    this(httpCode, composeIssue(IssueSeverity.ERROR, type, description));
  }

  public FhirException(int statusCode, OperationOutcomeIssueComponent issue) {
    this(statusCode, Collections.singletonList(issue));
  }

  public FhirException(int httpCode, List<OperationOutcomeIssueComponent> issues) {
    this.httpCode = httpCode;
    this.issues = issues;
  }

  public int getStatusCode() {
    return httpCode;
  }

  public List<OperationOutcomeIssueComponent> getIssues() {
    return issues;
  }

  public List<Extension> getExtensions() {
    return extensions;
  }

  public void addExtension(String url, String value) {
    Extension ext = new Extension();
    ext.setUrl("fullUrl");
    ext.setValue(new StringType(value));
    addExtension(ext);
  }

  public void addExtension(Extension extension) {
    if (extensions == null) {
      extensions = new ArrayList<>();
    }
    extensions.add(extension);
  }

  public void addContained(Resource resource) {
    if (contained == null) {
      contained = new ArrayList<>();
    }
    contained.add(resource);
  }

  public List<Resource> getContained() {
    return contained;
  }

  @Override
  public String getMessage() {
    return issues.stream().map(i -> i.getDetails().getText()).collect(joining(", "));
  }

  protected static OperationOutcomeIssueComponent composeIssue(IssueSeverity severity,
                                                               IssueType type,
                                                               String detailsText) {
    OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent();
    issue.setCode(type);
    issue.setSeverity(severity);

    CodeableConcept details = new CodeableConcept();
    details.setText(detailsText);

    issue.setDetails(details);
    return issue;
  }

}
