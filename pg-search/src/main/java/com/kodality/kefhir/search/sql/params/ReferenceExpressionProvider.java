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

import com.kodality.kefhir.core.model.search.QueryParam;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.search.sql.SqlToster;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.CodeType;

import static java.util.stream.Collectors.toList;

public class ReferenceExpressionProvider extends ExpressionProvider {
  private static ThreadLocalInteger I = new ThreadLocalInteger();

  @Override
  public SqlBuilder makeExpression(QueryParam param, String alias) {
    Validate.isTrue(param.getChains() == null);
    List<SqlBuilder> ors = param.getValues().stream().map(v -> reference(v, param, alias)).collect(toList());
    return new SqlBuilder().or(ors);
  }

  @Override
  public SqlBuilder order(String resourceType, String key, String alias) {
    return null; // TODO:
  }

  private static SqlBuilder reference(String value, QueryParam param, String alias) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(String.format("search.reference(%s, %s)", alias, path(param)));
    if (StringUtils.isEmpty(value)) {
      return sb.append(" = array[null]");
    }
    return sb.append(" @> array[?]::text[] ", value);
  }

  public static SqlBuilder chain(List<QueryParam> params, String parentAlias) {
    I.remove();
    SqlBuilder sb = new SqlBuilder();
    for (QueryParam param : params) {
      sb.append(chain(param, parentAlias));
    }
    return sb;
  }

  private static SqlBuilder chain(QueryParam param, String parentAlias) {
    SqlBuilder sb = new SqlBuilder();
    if (param.getChains() == null) {
      SqlBuilder condition = SqlToster.condition(param, parentAlias);
      if (condition != null && !condition.isEmpty()) {
        sb.and("(").append(condition).append(")");
      }
      return sb;
    }
    String[] refs = getReferencedTypes(param);
    String alias = generateAlias(refs.length == 1 ? refs[0].toLowerCase() : "friends");
    sb.append("INNER JOIN store.resource " + alias);
    sb.append(" ON ").in(alias + ".type", (Object[]) refs);
    String path = path(param);
    path = path.equals("'agent.who'") ? "'agent.whoReference'" : path;//XXX haha
    sb.and(String.format("search.reference(%s, %s) && search.ref(%s.type, %s.id)", parentAlias, path, alias, alias));
    sb.and(alias + ".sys_status = 'A'");
    sb.append(chain(param.getChains(), alias));
    return sb;
  }

  private static String generateAlias(String key) {
    Integer i = I.get(key);
    return key + (i == null ? "" : i);
  }

  private static String[] getReferencedTypes(QueryParam param) {
    if (param.getModifier() != null) {
      return new String[] { param.getModifier() };
    }
    return getParamTargets(param).toArray(new String[] {});
  }

  private static Set<String> getParamTargets(QueryParam param) {
    List<CodeType> targets = ConformanceHolder.requireSearchParam(param.getResourceType(), param.getKey()).getTarget();
    return targets.stream().map(c -> c.getValue()).collect(Collectors.toSet());
  }

  // private String fixReference(String input, QueryParam param) {
  // if(StringUtils.countMatches(input, "/") == 1){
  // return input;
  // }
  // SearchParameter confSearchParameter = SearchParameterMonitor.get(param.getResourceType(), param.getKey());
  // confSearchParameter.getTarget()
  // }

  private static class ThreadLocalInteger extends ThreadLocal<Map<String, Integer>> {
    @Override
    protected Map<String, Integer> initialValue() {
      return new HashMap<>();
    }

    public Integer get(String key) {
      Integer i = get().getOrDefault(key, 0);
      return get().put(key, ++i);
    }
  }

}
