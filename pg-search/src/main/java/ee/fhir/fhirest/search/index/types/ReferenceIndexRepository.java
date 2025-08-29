/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    return getValue(value, valueType).filter(v -> v != null && v.getId() != null);
  }

  private Stream<Value> getValue(Object value, String valueType) {
    switch (valueType) {
      case "canonical", "uri":
        return Stream.of(parseUri((String) value));
      case "Reference":
        String v = (String) ((Map) value).get("reference");
        return Stream.of(parseUri(v));
      case "Attachment":
        return Stream.empty(); //do nothing
      default:
        return Stream.empty();
    }
  }

  public static Value parseUri(String v) {
    String[] tokens = v.split("/");
    return new Value(
        tokens.length > 2 ? String.join("/", Arrays.copyOf(tokens, tokens.length - 2)) : null,
        tokens.length > 1 ? tokens[tokens.length - 2] : null,
        tokens[tokens.length - 1]
    );
  }

  @Override
  protected String fields() {
    return "base, type_id, id";
  }

  @Override
  protected SqlBuilder withValues(List<Value> values) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(values.stream().map(e -> "(?, search.resource_type_id(?), ?)").collect(joining(",")));
    sb.add(values.stream().flatMap(v -> Stream.of(v.getBase(), v.getType(), v.getId())).collect(Collectors.toList()));
    return sb;
  }

  @Getter
  @AllArgsConstructor
  public static class Value {
    private String base;
    private String type;
    private String id;
  }


}
