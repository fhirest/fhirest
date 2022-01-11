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
package com.kodality.kefhir.store;

import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.store.dao.ResourceFunctionsRepository;
import io.micronaut.runtime.event.annotation.EventListener;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@Singleton
@RequiredArgsConstructor
public class ResourceInitializer implements ConformanceUpdateListener {
  private final ResourceFunctionsRepository resourceFunctionsRepository;
  private final JdbcTemplate jdbcTemplate;

  @EventListener
  public void initConformanceResources(final Object event) {
    //XXX for some reason search_path is not set on first startup. quick hack.
    jdbcTemplate.update("set search_path to fhir,core,public");
    List.of("CapabilityStatement", "StructureDefinition", "SearchParameter", "OperationDefinition", "CompartmentDefinition")
        .forEach(r -> resourceFunctionsRepository.defineResource(r));
  }

  @Override
  public void updated() {
    String domainResource = "http://hl7.org/fhir/StructureDefinition/DomainResource";
    ConformanceHolder.getDefinitions().stream()
        .filter(def -> domainResource.equals(def.getBaseDefinition()) || def.getName().equals("Binary"))
        .forEach(d -> resourceFunctionsRepository.defineResource(d.getName()));
  }

}
