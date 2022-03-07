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

import com.kodality.kefhir.core.exception.FhirServerException;
import com.kodality.kefhir.core.model.search.QueryParam;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.search.repository.BlindexRepository;
import com.kodality.kefhir.search.util.SearchPathUtil;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toList;

public abstract class DefaultExpressionProvider extends ExpressionProvider {

  protected abstract SqlBuilder makeCondition(QueryParam param, String value);

  protected abstract String getOrderField();

  @Override
  public SqlBuilder order(String resourceType, String key, String alias) {
    String i = index(resourceType, key, alias);
    return new SqlBuilder("(SELECT " + getOrderField() + " FROM " + i + ")");
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
    List<String> paths = SearchPathUtil.parsePaths(expr).stream().filter(e -> e.startsWith(resourceType)).map(e -> RegExUtils.removeFirst(e, resourceType + "\\.")).collect(toList());
    if (paths.isEmpty()) {
   throw new FhirServerException(500, "config problem. path empty for param " + key);
    }
    return paths;
  }

  protected static String index(QueryParam param, String parentAlias) {
    return index(param.getResourceType(), param.getKey(), parentAlias);
  }

  protected static String index(String resourceType, String key, String parentAlias) {
    List<String> indexes = getPaths(resourceType, key).stream().map(p -> {
      String index = BlindexRepository.getIndex(resourceType, p);
      return String.format("search.%s i WHERE i.active = true and i.sid = %s.sid ", index, parentAlias);
    }).collect(toList());
    if (indexes.size() == 1) {
      return indexes.get(0);
    }
    return "(" + indexes.stream().map(i -> "select * from " + i).collect(Collectors.joining(" UNION ALL ")) + ") i where 1=1 ";
  }

}
