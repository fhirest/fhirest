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
package com.kodality.kefhir.search;

import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.search.model.StructureElement;
import com.kodality.kefhir.search.repository.ResourceStructureRepository;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;

import static java.util.stream.Collectors.toList;

@Singleton
@RequiredArgsConstructor
public class StructureDefinitionUpdater implements ConformanceUpdateListener {
  private final ResourceStructureRepository structureDefinitionRepository;
  private final BlindexInitializer blindexInitializer;

  @EventListener
  public void initConformanceResources(final StartupEvent event) {
    List.of("CapabilityStatement", "StructureDefinition", "SearchParameter", "OperationDefinition", "CompartmentDefinition", "ValueSet", "CodeSystem",
        "ConceptMap").forEach(r -> structureDefinitionRepository.defineResource(r));
    structureDefinitionRepository.refresh();
  }

  @Override
  public void updated() {
    List<String> resourceTypes = List.of("http://hl7.org/fhir/StructureDefinition/DomainResource", "http://hl7.org/fhir/StructureDefinition/Resource");
    ConformanceHolder.getDefinitions().stream()
        .filter(def -> def.getBaseDefinition() != null && resourceTypes.contains(def.getBaseDefinition()))
        .forEach(d -> structureDefinitionRepository.defineResource(d.getName()));

    structureDefinitionRepository.save(ConformanceHolder.getDefinitions().stream().flatMap(this::findElements).distinct().collect(toList()));
    structureDefinitionRepository.refresh();

    blindexInitializer.execute();
  }

  private Stream<StructureElement> findElements(StructureDefinition def) {
    return def.getSnapshot().getElement().stream().flatMap(el -> {
      if (el.getId().contains(":")) {
        // XXX think if we should and how should we store definitions with modifier (:). problem - they have same path
        // Quantity vs Quantity:simplequantity
        Stream.empty();
      }
      return el.getType().stream().map(t -> t.getCode()).distinct().map(type -> {
        String parent = StringUtils.substringBefore(el.getPath(), ".");
        String name = StringUtils.substringAfter(el.getPath(), ".");
        String child = StringUtils.replace(name, "[x]", StringUtils.capitalize(type));
        name = StringUtils.remove(name, "[x]");
        return new StructureElement(parent, child, name, type);
      });
    });
  }

  private static List<String> parents(String path) {
    List<String> result = new ArrayList<>();
    result.add(path);
    while (path.contains(".")) {
      path = StringUtils.substringBeforeLast(path, ".");
      result.add(path);
    }
    return result;
  }

  private boolean isMany(ElementDefinition elementDef) {
    if (elementDef.getMax() == null) {
      return false;
    }
    if (!elementDef.getPath().contains(".")) {// is root
      return false;
    }
    return elementDef.getMax().equals("*") || Integer.valueOf(elementDef.getMax()) > 1;
  }

}
