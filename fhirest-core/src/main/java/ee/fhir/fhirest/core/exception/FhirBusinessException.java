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

import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;

public class FhirBusinessException extends FhirException {

  public FhirBusinessException(int httpCode, String code, String description) {
    super(httpCode, composeIssue(code, description));
  }

  protected static OperationOutcomeIssueComponent composeIssue(String code, String detailsText) {
    OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent();
    issue.setCode(IssueType.BUSINESSRULE);
    issue.setSeverity(IssueSeverity.ERROR);

    CodeableConcept details = new CodeableConcept();
    details.addCoding().setCode(code);
    details.setText(detailsText);

    issue.setDetails(details);
    return issue;
  }

}
