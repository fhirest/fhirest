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
import ee.tehik.fhirest.util.sql.SqlBuilder;
import java.util.List;
import java.util.stream.Stream;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.joining;

@Slf4j
@Singleton
public class UriIndexRepository extends TypeIndexRepository<String> {

  @Override
  public String getType() {
    return "uri";
  }

  @Override
  public Stream<String> map(Object value, String valueType) {
    switch (valueType) {
      case "uri":
        return val(value);
      default:
        return null;
    }
  }

  protected static Stream<String> val(Object value) {
    if (value instanceof List list) {
      return list.stream();
    }
    return Stream.of((String) value);
  }

  @Override
  protected String fields() {
    return "uri";
  }

  @Override
  protected SqlBuilder withValues(List<String> values) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(values.stream().map(e -> "(?)").collect(joining(",")));
    sb.add(values);
    return sb;
  }
}
