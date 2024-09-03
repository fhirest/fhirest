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

package ee.fhir.fhirest.core.service.conformance;

import ee.fhir.fhirest.core.api.conformance.ConformanceUpdateListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.springframework.stereotype.Component;

@Component
public class CapabilitySearchConformance implements ConformanceUpdateListener {
  // resource type -> search param key -> search param
  private static final Map<String, Map<String, CapabilityStatementRestResourceSearchParamComponent>> params =
      new HashMap<>();

  public static CapabilityStatementRestResourceSearchParamComponent get(String resourceType, String element) {
    return params.getOrDefault(resourceType, params.getOrDefault(null, Collections.emptyMap())).get(element);
  }

  @Override
  public void updated() {
    CapabilitySearchConformance.setCapability(ConformanceHolder.getCapabilityStatement());
  }

  public static void setCapability(CapabilityStatement capability) {
    params.clear();
    if (capability == null) {
      return;
    }
    capability.getRest().forEach(rest -> {
      if (rest.getMode() == RestfulCapabilityMode.SERVER) {
        params.put(null, map(rest.getSearchParam(), p -> p.getName()));
        rest.getResource().forEach(resource -> read(resource));
      }
    });
  }

  private static void read(CapabilityStatementRestResourceComponent resource) {
    Map<String, CapabilityStatementRestResourceSearchParamComponent> globalParams = params.get(null);
    Map<String, CapabilityStatementRestResourceSearchParamComponent> all = new HashMap<>(globalParams);
    all.putAll(map(resource.getSearchParam(), p -> p.getName()));
    params.put(resource.getType(), all);
  }

  private static <K, V> Map<K, V> map(List<V> list, Function<V, K> key) {
    Map<K, V> map = new HashMap<>();
    list.forEach(v -> map.put(key.apply(v), v));
    return map;
  }

}
