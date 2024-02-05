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
 package ee.tehik.fhirest.auth.smart;

import ee.tehik.fhirest.core.exception.FhirException;
import lombok.Getter;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

@Getter
public class SmartScope {
  private final String context; //patient, user...
  private final String resourceType;
  private final String permissions;

  public SmartScope(String scope) {
    if (scope == null) {
      throw new FhirException(400, IssueType.PROCESSING, "empty auth scope arg");
    }

    int s = scope.indexOf("/");
    int d = scope.indexOf(".");

    if (s <= 0 || d <= s || d == scope.length()) {
      throw new FhirException(400, IssueType.PROCESSING, "invalid auth scope arg");
    }

    this.context = scope.substring(0, s);
    this.resourceType = scope.substring(s + 1, d);
    this.permissions = scope.substring(d + 1);
  }

}
