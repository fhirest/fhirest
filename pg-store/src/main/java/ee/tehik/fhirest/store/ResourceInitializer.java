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
package ee.tehik.fhirest.store;

import ee.tehik.fhirest.core.api.conformance.ConformanceUpdateListener;
import ee.tehik.fhirest.core.service.conformance.ConformanceHolder;
import ee.tehik.fhirest.store.repository.ResourceFunctionsRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResourceInitializer implements ConformanceUpdateListener {
  private final ResourceFunctionsRepository resourceFunctionsRepository;

  @Override
  public void updated() {
    List<String> resourceTypes = List.of("http://hl7.org/fhir/StructureDefinition/DomainResource", "http://hl7.org/fhir/StructureDefinition/Resource");
    ConformanceHolder.getDefinitions().stream()
        .filter(def -> def.getBaseDefinition() != null && resourceTypes.contains(def.getBaseDefinition()))
        .forEach(d -> resourceFunctionsRepository.defineResource(d.getName()));
  }

}
