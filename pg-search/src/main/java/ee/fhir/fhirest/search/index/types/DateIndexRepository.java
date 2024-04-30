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

package ee.fhir.fhirest.search.index.types;

import ee.fhir.fhirest.core.util.DateUtil;
import ee.fhir.fhirest.search.index.TypeIndexRepository;
import ee.fhir.fhirest.search.index.types.DateIndexRepository.Value;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.joining;

@Slf4j
@Component
public class DateIndexRepository extends TypeIndexRepository<Value> {

  @Override
  public String getType() {
    return "date";
  }

  @Override
  public Stream<Value> map(Object value, String valueType) {
    return getValue(value, valueType).filter(v -> v != null && (v.getStart() != null || v.getEnd() != null));
  }

  private Stream<Value> getValue(Object value, String valueType) {
    switch (valueType) {
      case "date":
      case "instant":
      case "dateTime":
        return Stream.of(new Value(value));
      case "Period":
        Object start = ((Map) value).get("start");
        Object end = ((Map) value).get("end");
        return Stream.of(new Value(start, end));
      default:
        return Stream.of();
    }
  }

  @Override
  protected String fields() {
    return "range";
  }

  @Override
  protected SqlBuilder withValues(List<Value> values) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(values.stream().map(e -> "(tstzrange(?,?, '[]'))").collect(joining(",")));
    sb.add(values.stream().flatMap(v -> Stream.of(v.getStart(), v.getEnd())).collect(Collectors.toList()));
    return sb;
  }

  @Getter
  public static class Value {
    private Date start;
    private Date end;

    public Value(Object date) {
      this.start = parse(date);
      this.end = start;
    }

    public Value(Object start, Object end) {
      this.start = parse(start);
      this.end = parse(end);
    }

    private static Date parse(Object value) {
      return DateUtil.parse((String) value);
    }

  }

}
