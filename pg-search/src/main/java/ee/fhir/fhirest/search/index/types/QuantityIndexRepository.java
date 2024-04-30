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
import ee.fhir.fhirest.search.index.types.QuantityIndexRepository.Value;
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
public class QuantityIndexRepository extends TypeIndexRepository<Value> {

  @Override
  public String getType() {
    return "quantity";
  }

  @Override
  public Stream<Value> map(Object value, String valueType) {
    return getValue((Map) value, valueType).filter(v -> v != null && (v.getLower() != null || v.getUpper() != null));
  }

  private Stream<Value> getValue(Map value, String valueType) {
    switch (valueType) {
      case "Quantity":
      case "Age":
        Map obj = value;
        return Stream.of(new Value(obj.get("value"), obj.get("value"), obj.get("system"), obj.get("code"), obj.get("unit")));
      case "SampledData":
        obj = value;
        return Stream.of(new Value(obj.get("lowerLimit"), obj.get("upperLimit"), null, null, null));
      case "Range":
        obj = value;
        return Stream.of(new Value(get(obj, "low"), get(obj, "high"), null, null, null));
      default:
        return Stream.of();
    }
  }

  private Object get(Map obj, String key) {
    if (!obj.containsKey(key)) {
      return null;
    }
    return ((Map) obj.get(key)).get("value");
  }

  @Override
  protected String fields() {
    return "range, system_id, code, unit";
  }

  @Override
  protected SqlBuilder withValues(List<Value> values) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(values.stream().map(e -> "(numrange(?,?, '[]'), search.system_id(?), ?, ?)").collect(joining(",")));
    sb.add(values.stream().flatMap(v -> Stream.of(v.getLower(), v.getUpper(), v.getSystem(), v.getCode(), v.getUnit())).collect(Collectors.toList()));
    return sb;
  }

  @Getter
  public static class Value {
    private BigDecimal lower;
    private BigDecimal upper;
    private String system;
    private String code;
    private String unit;

    public Value(Object lower, Object upper, Object system, Object code, Object unit) {
      this.lower = getValue(lower);
      this.upper = getValue(upper);
      this.system = (String) system;
      this.code = (String) code;
      this.unit = (String) unit;
    }

    private BigDecimal getValue(Object v) {
      if (v == null) {
        return null;
      }
      if (v instanceof BigDecimal) {
        return (BigDecimal) v;
      }
      if (v instanceof Double) {
        return BigDecimal.valueOf((Double) v);
      }
      if (v instanceof Long) {
        return BigDecimal.valueOf((Long) v);
      }
      if (v instanceof Integer) {
        return BigDecimal.valueOf((Integer) v);
      }
      return new BigDecimal(v.toString());
    }
  }

}
