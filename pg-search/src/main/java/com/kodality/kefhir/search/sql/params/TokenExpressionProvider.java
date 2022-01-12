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
 package com.kodality.kefhir.search.sql.params;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.search.QueryParam;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

public class TokenExpressionProvider extends ExpressionProvider {

  @Override
  public SqlBuilder makeExpression(QueryParam param, String alias) {
    List<SqlBuilder> ors = new ArrayList<>();
    for (String value : param.getValues()) {
      ors.add(token(value, param, alias));
    }
    return new SqlBuilder().or(ors);
  }

  @Override
  public SqlBuilder order(String resourceType, String key, String alias) {
    return new SqlBuilder("1"); // TODO:
  }

  private SqlBuilder token(String value, QueryParam param, String alias) {
    SqlBuilder sb = new SqlBuilder();
    if (StringUtils.equals(param.getModifier(), "not")) {
      throw new FhirException(400, IssueType.PROCESSING, ":not modifier not allowed in token param");
    }
    sb.append(String.format("search.token(%s, %s)", alias, path(param)));
    sb.append(" @> array[?]::text[] ", value.toLowerCase());
    return sb;
  }

}
