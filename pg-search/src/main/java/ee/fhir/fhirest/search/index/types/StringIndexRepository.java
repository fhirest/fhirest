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
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.joining;

@Slf4j
@Component
public class StringIndexRepository extends TypeIndexRepository<String> {

  @Override
  public String getType() {
    return "string";
  }

  @Override
  public Stream<String> map(Object value, String valueType) {
    switch (valueType) {
      case "string":
      case "markdown":
        return val(value);
      case "HumanName":
        Map obj = (Map) value;
        return Stream.concat(val(obj.get("family")), val(obj.get("given")));
      default:
        return Stream.of();
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
    return "string";
  }

  @Override
  protected SqlBuilder withValues(List<String> values) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(values.stream().map(e -> "(?)").collect(joining(",")));
    sb.add(values);
    return sb;
  }
}
