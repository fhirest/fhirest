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
import com.kodality.kefhir.search.util.FhirPathHackUtil;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public abstract class ExpressionProvider {

  public abstract SqlBuilder makeExpression(QueryParam param, String alias);

  public abstract SqlBuilder order(String resourceType, String key, String alias);

  protected static String path(QueryParam param) {
    return path(param.getResourceType(), param.getKey());
  }

  protected static String path(String resourceType, String key) {
    return "'" + getPath(resourceType, key) + "'";
  }

  private static String getPath(String resourceType, String key) {
    String expr = ConformanceHolder.requireSearchParam(resourceType, key).getExpression();
    String path = Stream.of(expr.split("\\|"))
        .map((s) -> StringUtils.trim(s))
        .filter(e -> e.startsWith(resourceType))
        .findFirst()
        .orElse(null);
    if (StringUtils.isEmpty(path)) {
      throw new FhirServerException(500, "config problem. path empty for param " + key);
    }
    path = FhirPathHackUtil.replaceAs(path);
    return StringUtils.removeFirst(path, resourceType + "\\.");
  }

  protected static String parasol(QueryParam param, String alias) {
    return parasol(param.getResourceType(), param.getKey(), alias);
  }

  protected static String parasol(String resourceType, String key, String alias) {
    String tblName = BlindexRepository.getParasol(resourceType, getPath(resourceType, key));
    return String.format(tblName + " WHERE resource_key = %s.key ", alias);
  }

}
