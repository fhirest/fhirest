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
