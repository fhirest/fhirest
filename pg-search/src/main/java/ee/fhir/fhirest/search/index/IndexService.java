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
}
