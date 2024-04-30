/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ee.fhir.fhirest.search.sql.params;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import org.apache.commons.lang3.StringUtils;

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
      throw new FhirException(FhirestIssue.FEST_035);
    }
    return super.makeExpression(param, alias);
  }

  @Override
  protected String getOrderField() {
    return "value";
  }

}
