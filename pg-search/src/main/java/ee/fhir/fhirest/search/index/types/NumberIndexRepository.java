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

import ee.fhir.fhirest.search.index.TypeIndexRepository;
import ee.fhir.fhirest.search.index.types.NumberIndexRepository.Value;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.math.BigDecimal;
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
public class NumberIndexRepository extends TypeIndexRepository<Value> {

  @Override
  public String getType() {
    return "number";
  }

  @Override
  public Stream<Value> map(Object value, String valueType) {
    switch (valueType) {
      case "integer":
      case "decimal":
        Map obj = (Map) value;
        return Stream.of(new Value(getValue(obj.get("lower")), getValue(obj.get("upper"))));
      default:
        return null;
    }
  }

  private BigDecimal getValue(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof BigDecimal) {
      return (BigDecimal) v;
    }
    return new BigDecimal(v.toString());
  }

  @Override
  protected String fields() {
    return "range";
  }

  @Override
  protected SqlBuilder withValues(List<Value> values) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(values.stream().map(e -> "(numrange(?, ?, '[]'))").collect(joining(",")));
    sb.add(values.stream().flatMap(v -> Stream.of(v.getLower(), v.getUpper())).collect(Collectors.toList()));
    return sb;
  }

  @Getter
  public static class Value {
    private BigDecimal lower;
    private BigDecimal upper;

    public Value(BigDecimal lower, BigDecimal upper) {
      this.lower = lower;
      this.upper = upper;
    }

    public Value(BigDecimal number) {
      this.lower = number;
      this.upper = this.lower;
    }
  }
}
