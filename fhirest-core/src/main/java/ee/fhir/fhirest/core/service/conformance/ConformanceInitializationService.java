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
import ee.fhir.fhirest.core.service.conformance.loader.ConformanceLoader;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.TerminologyCapabilities;
import org.springframework.stereotype.Component;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConformanceInitializationService {
  private final List<ConformanceUpdateListener> conformanceUpdateListeners;
  private final ConformanceLoader conformanceLoader;

  public void refresh() {
    log.info("refreshing conformance...");
    CompletableFuture.allOf(
        // TODO: think if there are several capability statements
        runAsync(() -> ConformanceHolder.setCapabilityStatement(
            this.<CapabilityStatement>load("CapabilityStatement").stream().min((o1, o2) -> o1.getId().equals("base") ? 0 : 1).orElse(null))),
        runAsync(() -> ConformanceHolder.setTerminologyCapabilities(
            this.<TerminologyCapabilities>load("TerminologyCapabilities").stream().findFirst().orElse(null))),
        runAsync(() -> ConformanceHolder.setStructureDefinitions(load("StructureDefinition"))),
        runAsync(() -> ConformanceHolder.setSearchParamGroups(load("SearchParameter"))),
        runAsync(() -> ConformanceHolder.setValueSets(load("ValueSet"))),
        runAsync(() -> ConformanceHolder.setCodeSystems(load("CodeSystem"))),
        runAsync(() -> ConformanceHolder.setCompartmentDefinitions(load("CompartmentDefinition"))),
        runAsync(() -> ConformanceHolder.setOperationDefinitions(load("OperationDefinition")))
    ).join();
    conformanceUpdateListeners.stream().sorted(Comparator.comparing(ConformanceUpdateListener::getOrder)).forEach(l -> l.updated());
    if (ConformanceHolder.getCapabilityStatement() != null) {
      log.info("conformance loaded");
    } else {
      log.info("conformance not initialized");
    }
  }

  protected <T extends Resource> List<T> load(String name) {
    return conformanceLoader.load(name);
  }

}
