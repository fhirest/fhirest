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
package com.kodality.kefhir.core.service.conformance;

import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.Resource;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class ConformanceInitializationService {
  private final List<ConformanceUpdateListener> conformanceUpdateListeners;
  private final ConformanceResourceLoader conformanceResourceLoader;

  public void refresh() {
    log.info("refreshing conformance...");
    CompletableFuture.allOf(
        runAsync(() -> ConformanceHolder.setCapabilityStatement(this.<CapabilityStatement>load("CapabilityStatement").stream()
            .filter(r -> r.getId().equals("base")) // TODO: think if there are several capability statements
            .findFirst().orElse(null))),
        runAsync(() -> ConformanceHolder.setStructureDefinitions(load("StructureDefinition"))),
        runAsync(() -> ConformanceHolder.setSearchParamGroups(load("SearchParameter"))),
        runAsync(() -> ConformanceHolder.setValueSets(load("ValueSet"))),
        runAsync(() -> ConformanceHolder.setCodeSystems(load("CodeSystem"))),
        runAsync(() -> ConformanceHolder.setCompartmentDefinitions(load("CompartmentDefinition")))
    ).join();
    conformanceUpdateListeners.stream().sorted(Comparator.comparing(ConformanceUpdateListener::getOrder)).forEach(l -> l.updated());
    if (ConformanceHolder.getCapabilityStatement() != null) {
      log.info("conformance loaded");
    } else {
      log.info("conformance not initialized");
    }
  }

  protected <T extends Resource> List<T> load(String name) {
    return conformanceResourceLoader.load(name);
  }

}
