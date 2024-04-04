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
 package ee.fhir.fhirest.core.service.resource;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import org.apache.commons.lang3.StringUtils;

public final class SearchUtil {
  /**
   * @return [resourceType, searchParam, (targetResourceType)]
   */
  public static String[] parseInclude(String include) {
    if (include == null) {
      return null;
    }
    String[] tokens = StringUtils.split(include, ":");
    if (tokens.length < 2 || tokens.length > 3) {
      throw new FhirException(FhirestIssue.FEST_029);
    }
    return new String[] { tokens[0], tokens[1], tokens.length > 2 ? tokens[2] : null };
  }

}
