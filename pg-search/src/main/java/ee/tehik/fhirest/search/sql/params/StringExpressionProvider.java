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
 package ee.tehik.fhirest.search.sql.params;

import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.core.model.search.QueryParam;
import ee.tehik.fhirest.util.sql.SqlBuilder;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

public class StringExpressionProvider extends DefaultExpressionProvider {

  @Override
  protected SqlBuilder makeCondition(QueryParam param, String value) {
    if (param.getModifier() == null) {
      return new SqlBuilder("i.string ilike ?", value + "%");
    }
    if (param.getModifier().equals("contains")) {
      return new SqlBuilder("i.string ilike ?", "%" + value + "%");
    }
    if (param.getModifier().equals("exact")) {
      return new SqlBuilder("i.string = ?", value);
    }

    throw new FhirException(400, IssueType.INVALID, "modifier " + param.getModifier() + " not supported");
  }

  @Override
  protected String getOrderField() {
    return "string";
  }
}
