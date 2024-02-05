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
package ee.tehik.fhirest.search.sql.params;

import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.core.model.search.QueryParam;
import ee.tehik.fhirest.search.sql.SearchPrefix;
import ee.tehik.fhirest.util.sql.SqlBuilder;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

public class NumberExpressionProvider extends DefaultExpressionProvider {
  private static final String[] operators = {null, SearchPrefix.le, SearchPrefix.lt, SearchPrefix.ge, SearchPrefix.gt, SearchPrefix.ne};
  private static final Map<Integer, BigDecimal> precisions = new HashMap<>() {
    @Override
    public BigDecimal get(Object key) {
      return computeIfAbsent((Integer) key, k -> new BigDecimal("0." + StringUtils.repeat('0', k) + "5"));
    }
  };

  @Override
  protected SqlBuilder makeCondition(QueryParam param, String value) {
    SearchPrefix prefix = SearchPrefix.parse(value, operators);
    BigDecimal number = new BigDecimal(prefix.getValue());

    if (prefix.getPrefix() == null) {
      return new SqlBuilder("i.range && numrange(?, ?, '[]')", lower(number), upper(number));
    }
    if (prefix.getPrefix().equals(SearchPrefix.ne)) {
      return new SqlBuilder("not (i.range && numrange(?, ?, '[]'))", lower(number), upper(number));
    }
    if (prefix.getPrefix().equals(SearchPrefix.lt)) {
      return new SqlBuilder("upper(i.range) < ? ", number);
    }
    if (prefix.getPrefix().equals(SearchPrefix.le)) {
      return new SqlBuilder("lower(i.range) < ? ", number);
    }
    if (prefix.getPrefix().equals(SearchPrefix.gt)) {
      return new SqlBuilder("lower(i.range) > ? ", number);
    }
    if (prefix.getPrefix().equals(SearchPrefix.ge)) {
      return new SqlBuilder("upper(i.range) > ? ", number);
    }

    throw new FhirException(400, IssueType.INVALID, "prefix " + prefix.getPrefix() + " not supported");
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
