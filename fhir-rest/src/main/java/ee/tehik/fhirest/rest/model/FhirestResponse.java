package ee.tehik.fhirest.rest.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class FhirestResponse {
  private Integer status;
  private Object body;
  private Map<String, List<String>> headers = new HashMap<>();

  public FhirestResponse(Integer status) {
    this.status = status;
  }

  public FhirestResponse(Integer status, Object body) {
    this.status = status;
    this.body = body;
  }

  public FhirestResponse status(Integer status) {
    this.status = status;
    return this;
  }

  public FhirestResponse body(Object body) {
    this.body = body;
    return this;
  }

  public FhirestResponse header(String name, String value) {
    headers.computeIfAbsent(name, x -> new ArrayList<>(1)).add(value);
    return this;
  }

  public String getHeader(String name) {
    return headers.containsKey(name) && !headers.get(name).isEmpty() ? headers.get(name).get(0) : null;
  }

}
