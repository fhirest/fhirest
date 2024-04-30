/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.search.repository.ResourceStructureRepository;
import ee.fhir.fhirest.search.sql.SearchSqlUtil;
import ee.fhir.fhirest.search.util.SearchPathUtil;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Enumeration;
import org.hl7.fhir.r5.model.Enumerations.VersionIndependentResourceTypesAll;

import static java.util.stream.Collectors.toSet;

public class ReferenceExpressionProvider extends DefaultExpressionProvider {
  private static ThreadLocalInteger I = new ThreadLocalInteger();

  @Override
  public SqlBuilder makeExpression(QueryParam param, String alias) {
    return super.makeExpression(param, alias);
  }

  @Override
  protected SqlBuilder makeCondition(QueryParam param, String v) {
    String type = StringUtils.contains(v, "/") ? StringUtils.substringBefore(v, "/") : null;
    String id = StringUtils.contains(v, "/") ? StringUtils.substringAfter(v, "/") : v;
    if (param.getModifier() != null) {
      if (type != null && !param.getModifier().equals(type)) {
        throw new FhirException(FhirestIssue.FEST_034, "value", param.getKey());
      }
      type = param.getModifier();
    }

    String expr = ConformanceHolder.requireSearchParam(param.getResourceType(), param.getKey()).getExpression();
    expr = Arrays.stream(expr.split("\\|")).map(e -> StringUtils.trim(e)).filter(e -> e.startsWith(param.getResourceType())).findFirst().orElse(null);
    String fhirPathType = SearchPathUtil.getFhirpathWhereResolveType(expr);
    if (fhirPathType != null) {
      if (type != null && !fhirPathType.equals(type)) {
        return new SqlBuilder("1 = 0");
      }
      type = fhirPathType;
    }

    SqlBuilder sb = new SqlBuilder();
    sb.append("(");
    sb.append("i.id = ?", id);
    sb.appendIfNotNull(" and i.type_id = search.rt_id(?)", type);
    sb.append(")");
    return sb;
  }

  @Override
  public SqlBuilder order(String resourceType, String key, String alias, String direction) {
    return null; // TODO:
  }

  @Override
  protected String getOrderField() {
    return null; // TODO:
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
    List<Enumeration<VersionIndependentResourceTypesAll>> targets = ConformanceHolder.requireSearchParam(param.getResourceType(), param.getKey()).getTarget();
    return targets.stream().map(c -> c.getValue().toCode()).collect(toSet());
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
