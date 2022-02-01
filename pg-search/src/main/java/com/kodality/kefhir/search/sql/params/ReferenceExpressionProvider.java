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
import com.kodality.kefhir.search.repository.ResourceStructureRepository;
import com.kodality.kefhir.search.sql.SearchSqlUtil;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeType;

import static java.util.stream.Collectors.toSet;

public class ReferenceExpressionProvider extends ExpressionProvider {
  private static ThreadLocalInteger I = new ThreadLocalInteger();

  @Override
  protected SqlBuilder makeCondition(QueryParam param, String v) {
    return reference(v);
  }

  @Override
  public SqlBuilder order(String resourceType, String key, String alias) {
    return null; // TODO:
  }

  private static SqlBuilder reference(String query) {
    String type = StringUtils.contains(query, "/") ? StringUtils.substringBefore(query, "/") : null;
    String id = StringUtils.contains(query, "/") ? StringUtils.substringAfter(query, "/") : query;
    SqlBuilder sb = new SqlBuilder();
    sb.append("(");
    sb.append("i.id = ?", id);
    sb.appendIfNotNull(" and i.type_id = search.resource_type_id(?)", type);
    sb.append(")");
    return sb;
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
      SqlBuilder condition = SearchSqlUtil.condition(param, parentAlias);
      if (condition != null && !condition.isEmpty()) {
        sb.and("(").append(condition).append(")");
      }
      return sb;
    }
    Set<String> refs = getReferencedTypes(param);
    String alias = generateAlias(refs.size() == 1 ? refs.iterator().next().toLowerCase() : "friends");
    sb.append("INNER JOIN search.resource " + alias);
    sb.append(" ON ").in(alias + ".resource_type", refs.stream().map(ResourceStructureRepository::getTypeId).collect(toSet()));
    sb.and(alias + ".active = true");
    sb.and(" EXISTS (SELECT 1 FROM " + index(param, parentAlias))
        .and(String.format("i.id = %s.resource_id", alias))
        .and(String.format("i.type_id = %s.resource_type", alias))
        .append(")");
    sb.append(chain(param.getChains(), alias));
    return sb;
  }

  private static String generateAlias(String key) {
    Integer i = I.get(key);
    return key + (i == null ? "" : i);
  }

  private static Set<String> getReferencedTypes(QueryParam param) {
    if (param.getModifier() != null) {
      return Set.of(param.getModifier());
    }
    return getParamTargets(param);
  }

  private static Set<String> getParamTargets(QueryParam param) {
    List<CodeType> targets = ConformanceHolder.requireSearchParam(param.getResourceType(), param.getKey()).getTarget();
    return targets.stream().map(c -> c.getValue()).collect(toSet());
  }

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
