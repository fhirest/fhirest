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
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.search.sql.ExpressionProvider;
import com.kodality.kefhir.search.sql.SearchSqlUtil;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.SearchParameter;

import static java.util.stream.Collectors.toList;

public class CompositeExpressionProvider extends ExpressionProvider {


  @Override
  public SqlBuilder makeExpression(QueryParam param, String alias) {
    SearchParameter sp = ConformanceHolder.getSearchParam(param.getResourceType(), param.getKey());
    List<SqlBuilder> ors = param.getValues().stream().filter(v -> !StringUtils.isEmpty(v)).map(v -> {
      String[] parts = StringUtils.split(v, "$");
      if (parts.length != sp.getComponent().size()) {
        throw new FhirException(400, IssueType.INVALID, "invalid composite parameter");
      }
      List<SqlBuilder> ands = sp.getComponent().stream().map(c -> {
        String p = parts[sp.getComponent().indexOf(c)];
        String subParamId = StringUtils.substringAfterLast(c.getDefinition(), "/");
        SearchParameter subParam = ConformanceHolder.getSearchParams().get(subParamId);
        // XXX: c.getExpression() is ignored.
        // looks like fhir tries to narrow-down search parameter expressions when using composite params.
        // we can't do that, however, because expressions are done on 'SearchParameter' level
        QueryParam qp = new QueryParam(subParam.getCode(), null, subParam.getType(), param.getResourceType());
        qp.setValues(List.of(p));
        return SearchSqlUtil.condition(qp, alias);
      }).collect(toList());
      return new SqlBuilder().append(ands, "AND");
    }).collect(toList());
    return new SqlBuilder().append(ors, "OR");
  }

  @Override
  public SqlBuilder order(String resourceType, String key, String alias, String direction) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "order by composite param not implemented");
  }
}
