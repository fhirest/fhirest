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
import com.kodality.kefhir.core.service.resource.ResourceSearchService;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Resource;

import static java.util.stream.Collectors.toList;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class ConformanceInitializationService {
  private final ResourceSearchService resourceSearchService;
  private final ResourceFormatService resourceFormatService;
  private final List<ConformanceUpdateListener> conformanceUpdateListeners;

  @EventListener
  @Async
  public void init(final StartupEvent event) {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    refresh();
  }

  public void refresh() {
    log.info("refreshing conformance...");
    ConformanceHolder.setCapabilityStatement(this.<CapabilityStatement>load("CapabilityStatement").stream().findFirst().orElse(null));
    ConformanceHolder.setStructureDefinitions(load("StructureDefinition"));
    ConformanceHolder.setSearchParams(load("SearchParameter"));
    conformanceUpdateListeners.forEach(l -> l.updated());
    log.info("conformance loaded");
  }

  private <T extends Resource> List<T> load(String name) {
    String[] p = {"_count", "9999"};
    return resourceSearchService.search(name, p).getEntries().stream().map(v -> resourceFormatService.<T>parse(v.getContent())).collect(toList());
  }

}