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
import ee.fhir.fhirest.search.index.types.ReferenceIndexRepository.Value;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.joining;

@Slf4j
@Component
public class ReferenceIndexRepository extends TypeIndexRepository<Value> {

  @Override
  public String getType() {
    return "reference";
  }

  @Override
  public Stream<Value> map(Object value, String valueType) {
    return getValue((Map) value, valueType).filter(v -> v != null && v.getId() != null);
  }

  private Stream<Value> getValue(Map value, String valueType) {
    switch (valueType) {
      case "Reference":
        String v = (String) value.get("reference");
        String type = StringUtils.contains(v, "/") ? StringUtils.substringBefore(v, "/") : null;
        String id = StringUtils.contains(v, "/") ? StringUtils.substringAfter(v, "/") : v;
        return Stream.of(new Value(type, id));
      case "Attachment":
        return Stream.empty(); //do nothing
      default:
        return Stream.empty();
    }
  }

  @Override
  protected String fields() {
    return "type_id, id";
  }

  @Override
  protected SqlBuilder withValues(List<Value> values) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(values.stream().map(e -> "(search.resource_type_id(?), ?)").collect(joining(",")));
    sb.add(values.stream().flatMap(v -> Stream.of(v.getType(), v.getId())).collect(Collectors.toList()));
    return sb;
  }

  @Getter
  @AllArgsConstructor
  public static class Value {
    private String type;
    private String id;
  }

}
