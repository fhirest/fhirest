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

package ee.fhir.fhirest.core.service.conformance.loader;

import ee.fhir.fhirest.core.util.BeanContext;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Resource;


/**
 * <p>Use this to provide preloaded conformance resources from file storage or elsewhere</p>
 * <pre>
 * {@code
 * @Component
 * @Primary
 * public class FileBasedConformanceLoader extends ConformanceStaticLoader implements ConformanceLoader {
 *   public List<String> getResources() {
 *     <load resources here>
 *   }
 * }
 * </pre>
 */
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
