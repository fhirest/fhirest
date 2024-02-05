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
package ee.tehik.fhirest.core.service.conformance.loader;

import ee.tehik.fhirest.core.util.BeanContext;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Resource;

public abstract class ConformanceStaticLoader implements ConformanceLoader {
  protected final Map<String, List<Resource>> resources = new HashMap<>();

  public abstract List<String> getResources();

  @PostConstruct
  public void init() {
    ResourceFormatService formatService = BeanContext.getBean(ResourceFormatService.class);
    getResources().forEach(res -> {
      Resource resource = formatService.parse(res);
      if (resource.getResourceType().name().equals("Bundle")) {
        ((Bundle) resource).getEntry().forEach(e -> {
          resources.computeIfAbsent(e.getResource().getResourceType().name(), (k) -> new ArrayList<>()).add(e.getResource());
        });
      } else {
        resources.computeIfAbsent(resource.getResourceType().name(), (k) -> new ArrayList<>()).add(resource);
      }
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Resource> List<T> load(String name) {
    return resources.containsKey(name) ? (List<T>) resources.get(name) : List.of();
  }

}
