/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
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

import ee.fhir.fhirest.core.exception.FhirServerException;
import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.search.repository.BlindexRepository;
import ee.fhir.fhirest.search.sql.ExpressionProvider;
import ee.fhir.fhirest.search.util.SearchPathUtil;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toList;

public abstract class DefaultExpressionProvider extends ExpressionProvider {

  protected abstract SqlBuilder makeCondition(QueryParam param, String value);

  protected abstract String getOrderField();

  @Override
  public SqlBuilder order(String resourceType, String key, String alias, String direction) {
    String i = index(resourceType, key, alias);
    return new SqlBuilder("(SELECT " + getOrderField() + " FROM " + i + " order by 1 " + direction + " limit 1)");
  }

  @Override
  public SqlBuilder makeExpression(QueryParam param, String alias) {
      List<SqlBuilder> ors = param.getValues().stream().filter(v -> !StringUtils.isEmpty(v)).map(v -> {
       SqlBuilder sb = new SqlBuilder("EXISTS (SELECT 1 FROM " + index(param, alias));
       sb.and(makeCondition(param, v));
       sb.append(")");
       return sb;
      }).collect(toList());
      return new SqlBuilder().append(ors, "OR");
  }

  private static List<String> getPaths(String resourceType, String key) {
    String expr = ConformanceHolder.requireSearchParam(resourceType, key).getExpression();
    List<String> paths = SearchPathUtil.parsePaths(expr).stream()
        .filter(e -> e.startsWith(resourceType + "."))
        .map(e -> RegExUtils.removeFirst(e, resourceType + "\\."))
        .collect(toList());
    if (paths.isEmpty()) {
      throw new FhirServerException("config problem. path empty for param " + key);
    }
    return paths;
  }

  protected static String index(QueryParam param, String parentAlias) {
    return index(param.getResourceType(), param.getKey(), parentAlias);
  }

  protected static String index(String resourceType, String key, String parentAlias) {
    List<String> indexes = getPaths(resourceType, key).stream().map(p -> {
      String fullyQualifiedTableName = BlindexRepository.getIndex(resourceType, p); 
      return String.format("%s i WHERE i.active = true and i.sid = %s.sid ", fullyQualifiedTableName, parentAlias);
    }).collect(toList());
    if (indexes.size() == 1) {
      return indexes.get(0);
    }
    return "(" + indexes.stream()
        .map(i -> "select * from " + i)
        .collect(Collectors.joining(" UNION ALL ")) + ") i where 1=1 ";
  }

}
