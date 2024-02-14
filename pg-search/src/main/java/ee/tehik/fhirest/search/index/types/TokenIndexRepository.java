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

import ee.tehik.fhirest.search.index.TypeIndexRepository;
import ee.tehik.fhirest.search.index.types.TokenIndexRepository.Value;
import ee.tehik.fhirest.util.sql.SqlBuilder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.joining;

@Slf4j
@Component
public class TokenIndexRepository extends TypeIndexRepository<Value> {

  @Override
  public String getType() {
    return "token";
  }

  @Override
  public Stream<Value> map(Object value, String valueType) {
    return getValue(value, valueType).filter(v -> v != null && v.getValue() != null);
  }

  private Stream<Value> getValue(Object value, String valueType) {
    switch (valueType) {
      case "code":
      case "string":
        return Stream.of(new Value(null, (String) value));
      case "boolean":
        return Stream.of(new Value(null, ((Boolean) value).toString()));
      case "Coding":
      case "Identifier":
      case "ContactPoint":
        Map obj = (Map) value;
        return Stream.of(new Value((String) obj.get("system"), (String) obj.get("value")));
      case "CodeableConcept":
        List<Map> coding = (List<Map>) ((Map) value).get("coding");
        return coding == null ? Stream.of() : coding.stream().map(c -> new Value((String) c.get("system"), (String) c.get("code")));
      default:
        return Stream.of();
    }
  }

  @Override
  protected String fields() {
    return "system_id, value";
  }

  @Override
  protected SqlBuilder withValues(List<Value> values) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(values.stream().map(e -> "(search.system_id(?), ?)").collect(joining(",")));
    sb.add(values.stream().flatMap(v -> Stream.of(v.getSystem(), v.getValue())).collect(Collectors.toList()));
    return sb;
  }

  @Getter
  @AllArgsConstructor
  public static class Value {
    private String system;
    private String value;
  }

}
