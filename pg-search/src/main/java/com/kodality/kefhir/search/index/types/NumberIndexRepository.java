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
package com.kodality.kefhir.search.index.types;

import com.kodality.kefhir.search.index.TypeIndexRepository;
import com.kodality.kefhir.search.index.types.NumberIndexRepository.Value;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.joining;

@Slf4j
@Singleton
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
