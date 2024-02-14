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
package ee.tehik.fhirest.search.index;

import ee.tehik.fhirest.core.model.ResourceId;
import ee.tehik.fhirest.core.model.ResourceVersion;
import ee.tehik.fhirest.core.util.JsonUtil;
import ee.tehik.fhirest.search.StructureDefinitionHolder;
import ee.tehik.fhirest.search.model.Blindex;
import ee.tehik.fhirest.search.model.StructureElement;
import ee.tehik.fhirest.search.repository.BlindexRepository;
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
