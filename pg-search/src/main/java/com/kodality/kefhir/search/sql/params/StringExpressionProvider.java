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
import java.util.List;
import java.util.function.Function;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

import static java.util.stream.Collectors.joining;

public class StringExpressionProvider extends ExpressionProvider {
  private static final char S = '`';// separator

  @Override
  public SqlBuilder makeExpression(QueryParam param, String alias) {
    String field = String.format("search.string(%s, %s)", alias, path(param));
    SqlBuilder sb = new SqlBuilder(field);

    if (param.getModifier() == null) {
      return sb.append(" ~* ?", any(param.getValues(), v -> S + v));
    }
    if (param.getModifier().equals("contains")) {
      return sb.append(" ~* ?", any(param.getValues(), v -> v));
    }
    if (param.getModifier().equals("exact")) {
      return sb.append(" ~ ?", any(param.getValues(), v -> S + v + S));
    }

    throw new FhirException(400, IssueType.INVALID, "modifier " + param.getModifier() + " not supported");
  }

  @Override
  public SqlBuilder order(String resourceType, String key, String alias) {
    return new SqlBuilder(String.format("search.string(%s, %s)", alias, path(resourceType, key)));
  }

  private static String any(List<String> values, Function<String, String> mapper) {
    return values.stream().map(mapper).collect(joining("|"));
  }

}
