/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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
