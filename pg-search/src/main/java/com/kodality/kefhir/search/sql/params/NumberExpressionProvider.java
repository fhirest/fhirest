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
import com.kodality.kefhir.search.sql.SearchPrefix;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class NumberExpressionProvider extends ExpressionProvider {
  private static final Map<Integer, BigDecimal> precisions = new HashMap<>() {
    @Override
    public BigDecimal get(Object key) {
      return computeIfAbsent((Integer) key, k -> new BigDecimal("0." + StringUtils.repeat('0', k) + "5"));
    }
  };

  private static final Map<String, String> operators = new HashMap<>();
  static {
    operators.put(null, "=");
    operators.put(SearchPrefix.le, "<=");
    operators.put(SearchPrefix.lt, "<");
    operators.put(SearchPrefix.ge, ">=");
    operators.put(SearchPrefix.gt, ">");
    operators.put(SearchPrefix.ne, "<>");
  }

  @Override
  protected SqlBuilder makeCondition(QueryParam param, String value) {
    SearchPrefix prefix = SearchPrefix.parse(value, operators.keySet());
    String op = operators.get(prefix.getPrefix());
    BigDecimal number = new BigDecimal(prefix.getValue());

    if (prefix.getPrefix() == null) {
      return new SqlBuilder("i.number BETWEEN ? AND ?", lower(number), upper(number));
    }
    return new SqlBuilder("i.number " + op + "?", number);
  }

  @Override
  protected String getOrderField() {
    return "number";
  }

  private static BigDecimal lower(BigDecimal number) {
    return number.subtract(precisions.get(number.scale()));
  }

  private static BigDecimal upper(BigDecimal number) {
    return number.add(precisions.get(number.scale()));
  }

}
