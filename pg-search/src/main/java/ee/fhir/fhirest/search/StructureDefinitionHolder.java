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
package ee.fhir.fhirest.search;

import ee.fhir.fhirest.core.api.conformance.ConformanceUpdateListener;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.search.model.StructureElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.StructureDefinition;

@Component
@RequiredArgsConstructor
public class StructureDefinitionHolder implements ConformanceUpdateListener {
  private Map<String, Map<String, List<StructureElement>>> structures;

  public Map<String, Map<String, List<StructureElement>>> getStructureElements() {
    return structures;
  }

  @Override
  public Integer getOrder() {
    return 10;
  }

  @Override
  public void updated() {
    List<StructureElement> elements = ConformanceHolder.getDefinitions().stream().flatMap(this::findElements).distinct().collect(Collectors.toList());
    Map<String, List<StructureElement>> roots = elements.stream().collect(Collectors.groupingBy(e -> e.getParent()));

    List<StructureElement> result = new ArrayList<>();
    elements.forEach(el -> result.addAll(expand(el, roots)));

    this.structures = new HashMap<>();
    result.forEach(el -> {
      this.structures.computeIfAbsent(el.getParent(), a -> new HashMap<>()).computeIfAbsent(el.getChild(), a -> new ArrayList<>()).add(el);
      if (el.getAlias() != null && !el.getAlias().equals(el.getChild())) {
        this.structures.computeIfAbsent(el.getParent(), a -> new HashMap<>()).computeIfAbsent(el.getAlias(), a -> new ArrayList<>()).add(el);
      }
    });
  }

  private List<StructureElement> expand(StructureElement element, Map<String, List<StructureElement>> roots) {
    List<StructureElement> result = new ArrayList<>(1);
    result.add(element);
    if (!List.of("Extension", "Id", "Coding", "Identifier", "string").contains(element.getType()) && roots.containsKey(element.getType())) {
      List<StructureElement> children = roots.get(element.getType());
      children.forEach(c -> {
        StructureElement cs = new StructureElement(element.getParent(), element.getChild() + "." + c.getChild(), null, c.getType());
        result.addAll(expand(cs, roots));
      });
    }
    return result;
  }

  private Stream<StructureElement> findElements(StructureDefinition def) {
    return def.getSnapshot().getElement().stream().flatMap(el -> {
      if (el.getId().contains(":")) {
        // XXX think if we should and how should we store definitions with modifier (:). problem - they have same path
        // Quantity vs Quantity:simplequantity
        return Stream.empty();
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

}
