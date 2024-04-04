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
package ee.fhir.fhirest.core.exception;

import ee.fhir.fhirest.core.util.MapUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.text.StringSubstitutor;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StringType;

import static java.util.stream.Collectors.joining;


@Getter
public class FhirException extends RuntimeException {
  private final int httpCode;
  private final List<OperationOutcomeIssueComponent> issues;
  private List<Extension> extensions;
  private List<Resource> contained;

  public FhirException(FhirestIssue fi, Object... params) {
    this(fi, MapUtil.toMap(params));
  }

  public FhirException(FhirestIssue fi, Map<String, Object> params) {
    this(fi.getHttpCode(), composeIssue(IssueSeverity.ERROR, fi.getType(), fi.name(), substituteParams(fi.getDescription(), params)));
  }

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

  public void addExtension(String url, String value) {
    Extension ext = new Extension();
    ext.setUrl(url);
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

  @Override
  public String getMessage() {
    return issues.stream().map(i -> i.getDetails().getText()).collect(joining(", "));
  }

  protected static OperationOutcomeIssueComponent composeIssue(IssueSeverity severity, IssueType type, String detailsText) {
    return composeIssue(severity, type, null, detailsText);
  }

  protected static OperationOutcomeIssueComponent composeIssue(IssueSeverity severity, IssueType type, String code, String detailsText) {
    CodeableConcept details = new CodeableConcept().setText(detailsText);
    if (code != null) {
      details.addCoding(new Coding().setCode(code));
    }
    return new OperationOutcomeIssueComponent(severity, type).setDetails(details);
  }

  protected static String substituteParams(String text, Map<String, Object> params) {
    return StringSubstitutor.replace(text, params, "{{", "}}");
  }

}
