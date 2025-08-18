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

package ee.fhir.fhirest.search.index;

import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.util.JsonUtil;
import ee.fhir.fhirest.search.StructureDefinitionHolder;
import ee.fhir.fhirest.search.model.Blindex;
import ee.fhir.fhirest.search.model.StructureElement;
import ee.fhir.fhirest.search.repository.BlindexRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;

@Component
public class IndexService {
  private final SearchIndexRepository searchIndexRepository;
  private final Map<String, TypeIndexRepository> indexRepos;
  private final StructureDefinitionHolder structureDefinitionHolder;

  public IndexService(SearchIndexRepository searchIndexRepository, List<TypeIndexRepository> repos, StructureDefinitionHolder structureDefinitionHolder) {
    this.searchIndexRepository = searchIndexRepository;
    this.indexRepos = repos.stream().collect(Collectors.toMap(TypeIndexRepository::getType, r -> r));
    this.structureDefinitionHolder = structureDefinitionHolder;
  }

  public void saveIndexes(ResourceVersion version) {
    Long sid = searchIndexRepository.saveResource(version);
    saveIndexes(sid, version, BlindexRepository.getIndexes(version.getId().getResourceType()));
  }

  public void saveIndex(ResourceVersion version, Blindex blindex) {
    if (indexRepos.containsKey(blindex.getParamType())) {
      Long sid = searchIndexRepository.getResourceSid(version.getId());
      saveIndexes(sid, version, List.of(blindex));
    }
  }

  private <T> void saveIndexes(Long sid, ResourceVersion version, List<Blindex> blindexes) {
    Map<String, Object> jsonObject = version == null ? null : JsonUtil.fromJson(version.getContent().getValue());
    blindexes.forEach(blindex -> {
      if (!indexRepos.containsKey(blindex.getParamType())) {
        return;
      }
      TypeIndexRepository<T> repo = indexRepos.get(blindex.getParamType());

      // special-case: QuestionnaireResponse is-subject-ref (valueReference under items with isSubject extension)
      if ("QuestionnaireResponse".equals(blindex.getResourceType())
          && "reference".equals(blindex.getParamType())
          && blindex.getPath().contains("questionnaireresponse-isSubject")
          && blindex.getPath().contains(".answer.valueReference")) {

        List<Map<String, Object>> rawRefs = collectIsSubjectAnswerValueReferences(jsonObject);
        List<T> values = rawRefs.stream()
            .flatMap(obj -> repo.map(obj, "Reference"))
            .filter(Objects::nonNull)
            .collect(toList());
        repo.save(sid, version, blindex, values);
        return;
      }

      List<StructureElement> elements = structureDefinitionHolder.getStructureElements().get(blindex.getResourceType()).get(blindex.getPath());
      List<T> values = elements.stream().flatMap(el -> {
        return JsonUtil.fhirpathSimple(jsonObject, el.getChild()).flatMap(obj -> repo.map(obj, el.getType())).filter(Objects::nonNull);
      }).collect(toList());
      repo.save(sid, version, blindex, values);
    });
  }

  public void deleteResource(ResourceId id) {
    Long sid = searchIndexRepository.deleteResource(id);
    if (sid != null) {
      saveIndexes(sid, null, BlindexRepository.getIndexes(id.getResourceType()));
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> collectIsSubjectAnswerValueReferences(Map<String, Object> qrJson) {
    List<Map<String, Object>> out = new ArrayList<>();
    if (qrJson == null) return out;
    Object items = qrJson.get("item");
    if (!(items instanceof List<?> list)) return out;

    for (Object o : list) {
      if (!(o instanceof Map<?, ?> m)) continue;
      Map<String, Object> item = (Map<String, Object>) m;

      boolean isSubject = hasIsSubjectExtension(item);
      if (isSubject) {
        Object answers = item.get("answer");
        if (answers instanceof List<?> alist) {
          for (Object a : alist) {
            if (a instanceof Map<?, ?> am) {
              Map<String, Object> ans = (Map<String, Object>) am;
              Object vr = ans.get("valueReference");
              if (vr instanceof Map<?, ?> vrm) {
                out.add((Map<String, Object>) vrm);
              }
            }
          }
        }
      }

      Object nested = item.get("item");
      if (nested instanceof List<?>) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("item", nested);
        out.addAll(collectIsSubjectAnswerValueReferences(wrapper));
      }
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private static boolean hasIsSubjectExtension(Map<String, Object> item) {
    Object exts = item.get("extension");
    if (!(exts instanceof List<?> list)) return false;
    for (Object e : list) {
      if (e instanceof Map<?, ?> em) {
        Map<String, Object> ext = (Map<String, Object>) em;
        if ("http://hl7.org/fhir/StructureDefinition/questionnaireresponse-isSubject".equals(ext.get("url"))
            && Boolean.TRUE.equals(ext.get("valueBoolean"))) {
          return true;
        }
      }
    }
    return false;
  }
}