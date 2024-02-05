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
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

public class TokenExpressionProvider extends DefaultExpressionProvider {

  @Override
  protected SqlBuilder makeCondition(QueryParam param, String v) {
    String system = StringUtils.contains(v, "|") ? StringUtils.substringBefore(v, "|") : null;
    String value = StringUtils.contains(v, "|") ? StringUtils.substringAfter(v, "|") : v;
    value = StringUtils.isBlank(value) ? null : value;
    SqlBuilder sb = new SqlBuilder();
    sb.append("(");
    sb.appendIfNotNull("i.value = ?", value);
    sb.appendIfTrue(value != null && system != null, " and ");
    sb.appendIfNotNull("i.system_id = search.sys_id(?)", system);
    sb.append(")");
    return sb;
  }

  @Override
  public SqlBuilder makeExpression(QueryParam param, String alias) {
    if (StringUtils.equals(param.getModifier(), "not")) {
      throw new FhirException(400, IssueType.PROCESSING, ":not modifier not allowed in token param");
    }
    return super.makeExpression(param, alias);
  }

  @Override
  protected String getOrderField() {
    return "value";
  }

}
