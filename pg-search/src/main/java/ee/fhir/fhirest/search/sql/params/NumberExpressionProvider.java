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
import ee.fhir.fhirest.search.sql.SearchPrefix;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

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
    throw new FhirException(FhirestIssue.FEST_001, "desc", "prefix " + prefix.getPrefix());
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
