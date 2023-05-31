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
 package com.kodality.kefhir.core.service.resource;

import com.kodality.kefhir.core.exception.FhirException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

public final class SearchUtil {
  /**
   * @return [resourceType, searchParam, (targetResourceType)]
   */
  public static String[] parseInclude(String include) {
    if (include == null) {
      return null;//fdsfds
    }
    String[] tokens = StringUtils.split(include, ":");
    if (tokens.length < 2 || tokens.length > 3) {
      String details = "_include parameter invalid. ResourceType:SearchParameter[:targetResourceType]";
      throw new FhirException(400, IssueType.PROCESSING, details);
    }
    return new String[] { tokens[0], tokens[1], tokens.length > 2 ? tokens[2] : null };
  }

}
