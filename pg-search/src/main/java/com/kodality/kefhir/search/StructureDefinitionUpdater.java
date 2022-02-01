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
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;

@Singleton
@RequiredArgsConstructor
public class StructureDefinitionUpdater implements ConformanceUpdateListener {
  private final ResourceStructureRepository structureDefinitionRepository;
  private final BlindexInitializer blindexInitializer;

  @EventListener
  public void initConformanceResources(final StartupEvent event) {
    List.of("CapabilityStatement", "StructureDefinition", "SearchParameter", "OperationDefinition", "CompartmentDefinition")
        .forEach(r -> structureDefinitionRepository.defineResource(r));
    structureDefinitionRepository.refresh();
  }

  @Override
  public void updated() {
    String domainResource = "http://hl7.org/fhir/StructureDefinition/DomainResource";
    ConformanceHolder.getDefinitions().stream()
        .filter(def -> domainResource.equals(def.getBaseDefinition()) || def.getName().equals("Binary"))
        .forEach(d -> structureDefinitionRepository.defineResource(d.getName()));

    // TODO: check if already up to date
    structureDefinitionRepository.deleteAll();
    ConformanceHolder.getDefinitions().forEach(d -> saveDefinition(d));
    structureDefinitionRepository.refresh();

    blindexInitializer.execute();
  }

  private void saveDefinition(StructureDefinition def) {
    List<String> many = new ArrayList<>();
    List<StructureElement> elements = new ArrayList<>(def.getSnapshot().getElement().size());
    for (ElementDefinition elementDef : def.getSnapshot().getElement()) {
      if (elementDef.getId().contains(":")) {
        // XXX think if we should and how should we store definitions with modifier (:). problem - they have same path
        // Quantity vs Quantity:simplequantity
        return;
      }

      elementDef.getType().stream().map(t -> t.getCode()).distinct().forEach(type -> {
        String path = StringUtils.replace(elementDef.getPath(), "[x]", StringUtils.capitalize(type));
        elements.add(new StructureElement(StringUtils.substringBefore(path, "."),
                                          StringUtils.substringAfter(path, "."),
                                          type));
      });
      if (isMany(elementDef)) {
        many.add(elementDef.getPath());
      }
    }
    for (StructureElement element : elements) {
      element.setMany(CollectionUtils.containsAny(many, parents(element.getPath())));
    }

    structureDefinitionRepository.create(elements);
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
