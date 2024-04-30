/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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
