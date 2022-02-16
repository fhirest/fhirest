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
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toList;

public abstract class ExpressionProvider {

  protected abstract SqlBuilder makeCondition(QueryParam param, String value);

  public abstract SqlBuilder order(String resourceType, String key, String alias);

  public SqlBuilder makeExpression(QueryParam param, String alias) {
      List<SqlBuilder> ors = param.getValues().stream().filter(v -> !StringUtils.isEmpty(v)).map(v -> {
       SqlBuilder sb = new SqlBuilder("EXISTS (SELECT 1 FROM " + index(param, alias));
       sb.and(makeCondition(param, v));
       sb.append(")");
       return sb;
      }).collect(toList());
      return new SqlBuilder().or(ors);
  }


  protected static String path(QueryParam param) {
    return path(param.getResourceType(), param.getKey());
  }

  protected static String path(String resourceType, String key) {
    return "'" + getPath(resourceType, key) + "'";
  }

  private static String getPath(String resourceType, String key) {
    String expr = ConformanceHolder.requireSearchParam(resourceType, key).getExpression();
    String path = SearchPathUtil.parsePaths(expr).stream().filter(e -> e.startsWith(resourceType)).findFirst()
        .orElseThrow(() -> new FhirServerException(500, "config problem. path empty for param " + key));
    return RegExUtils.removeFirst(path, resourceType + "\\.");
  }

  protected static String index(QueryParam param, String parentAlias) {
    return index(param.getResourceType(), param.getKey(), parentAlias);
  }

  protected static String index(String resourceType, String key, String parentAlias) {
    String tblName = BlindexRepository.getIndex(resourceType, getPath(resourceType, key));
    return String.format("search.%s i WHERE i.active = true and i.sid = %s.sid ", tblName, parentAlias);
  }

}
