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
package ee.tehik.fhirest.search.index.types;

import ee.tehik.fhirest.core.util.DateUtil;
import ee.tehik.fhirest.search.index.TypeIndexRepository;
import ee.tehik.fhirest.search.index.types.DateIndexRepository.Value;
import ee.tehik.fhirest.util.sql.SqlBuilder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.joining;

@Slf4j
@Singleton
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
