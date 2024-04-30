/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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
import ee.fhir.fhirest.core.util.DateUtil;
import ee.fhir.fhirest.search.sql.SearchPrefix;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toList;

public class DateExpressionProvider extends DefaultExpressionProvider {
  private static final Map<Integer, String> intervals;
  private static final String[] operators = {null, SearchPrefix.eq, SearchPrefix.ne, SearchPrefix.le, SearchPrefix.lt, SearchPrefix.ge, SearchPrefix.gt};

  static {
    intervals = new HashMap<>();
    intervals.put(1, "1 year");
    intervals.put(2, "1 month");
    intervals.put(3, "1 day");
    intervals.put(4, "1 hour");
    intervals.put(5, "1 minute");
    intervals.put(6, "1 second");
    intervals.put(7, "1 second");
    intervals.put(8, "1 second");
  }

  @Override
  protected SqlBuilder makeCondition(QueryParam param, String v) {
    return new SqlBuilder(rangeSql("i.range", v));
  }

  @Override
  protected String getOrderField() {
    return "range";
  }

  public static SqlBuilder makeExpression(String field, QueryParam param) {
    List<SqlBuilder> ors = param.getValues().stream().filter(v -> !StringUtils.isEmpty(v))
        .map(v -> new SqlBuilder(rangeSql(field, v)))
        .collect(toList());
    return new SqlBuilder().append(ors, "OR");
  }

  private static String rangeSql(String field, String value) {
    SearchPrefix prefix = SearchPrefix.parse(value, operators);
    String search = range(prefix.getValue());
    if (prefix.getPrefix() == null || prefix.getPrefix().equals(SearchPrefix.eq)) {
      return field + " && " + search;
    }
    if (prefix.getPrefix().equals(SearchPrefix.ne)) {
      return "not (" + field + " && " + search + ")";
    }
    if (prefix.getPrefix().equals(SearchPrefix.lt)) {
      return field + " << " + search;
    }
    if (prefix.getPrefix().equals(SearchPrefix.gt)) {
      return field + " >> " + search;
    }
    if (prefix.getPrefix().equals(SearchPrefix.le)) {
      return "(" + field + " && " + search + " OR " + field + " << " + search + ")";
    }
    if (prefix.getPrefix().equals(SearchPrefix.ge)) {
      return "(" + field + " && " + search + " OR " + field + " >> " + search + ")";
    }
    throw new FhirException(FhirestIssue.FEST_001, "desc", "prefix " + prefix.getPrefix());
  }

  private static String range(String value) {
    try {
      value = StringUtils.replace(value, "Z", "+00:00");
      String[] input = StringUtils.split(value, "-T:+");
      if (value.contains("+")) { //with specified timezone
        String date = String.format("%s-%s-%sT%s:%s:%s+%s:%s", (Object[]) maskTz(input));
        DateUtil.parse(date, DateUtil.ISO_DATETIME).orElseThrow(() -> new IllegalArgumentException("Cannot parse date " + date)); //just for validation
        String interval = intervals.get(input.length);
        return "search.range('" + date + "', '" + interval + "')";
      } else { //with system timezone
        String date = String.format("%s-%s-%sT%s:%s:%s", (Object[]) mask(input));
        date = LocalDateTime.parse(date).atZone(ZoneId.systemDefault()).toOffsetDateTime().format(DateTimeFormatter.ISO_DATE_TIME);
        String interval = intervals.get(input.length);
        return "search.range('" + date + "', '" + interval + "')";
      }
    } catch (DateTimeParseException e) {
      throw new FhirException(FhirestIssue.FEST_032, "date", value);
    }
  }

  private static String[] maskTz(String[] input) {
    String[] mask = new String[]{"0000", "01", "01", "00", "00", "00", "00", "00"};
    System.arraycopy(input, 0, mask, 0, input.length);
    return mask;
  }

  private static String[] mask(String[] input) {
    String[] mask = new String[]{"0000", "01", "01", "00", "00", "00"};
    System.arraycopy(input, 0, mask, 0, input.length);
    return mask;
  }

}
