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
import ee.fhir.fhirest.search.index.types.TokenIndexRepository.Value;
import ee.fhir.fhirest.util.sql.SqlBuilder;
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
