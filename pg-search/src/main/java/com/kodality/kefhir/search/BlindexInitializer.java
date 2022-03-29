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

import com.kodality.kefhir.core.model.search.SearchResult;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.core.service.resource.ResourceSearchService;
import com.kodality.kefhir.search.model.Blindex;
import com.kodality.kefhir.search.repository.BlindexRepository;
import com.kodality.kefhir.search.util.SearchPathUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.postgresql.util.PSQLException;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class BlindexInitializer {
  private final BlindexRepository blindexRepository;
  private final ResourceSearchService resourceSearchService;

  public Object execute() {
    List<String> resourceTypes = List.of("http://hl7.org/fhir/StructureDefinition/DomainResource", "http://hl7.org/fhir/StructureDefinition/Resource");
    List<String> defined = ConformanceHolder.getDefinitions().stream()
        .filter(def -> def.getBaseDefinition() != null && resourceTypes.contains(def.getBaseDefinition()))
        .map(def -> def.getName()).collect(Collectors.toList());
    if (CollectionUtils.isEmpty(defined)) {
      log.error("blindex: will not run. definitions either empty, either definitions not yet loaded.");
      return null;
    }
    Map<String, Blindex> create =
        ConformanceHolder.getSearchParams().values().stream()
            .filter(sp -> sp.getExpression() != null && sp.getType() != SearchParamType.COMPOSITE)
            .flatMap(sp -> SearchPathUtil.parsePaths(sp.getExpression()).stream()
                .filter(s -> defined.contains(StringUtils.substringBefore(s, ".")))
                .map(s -> new Blindex(sp.getType().toCode(), StringUtils.substringBefore(s, "."), StringUtils.substringAfter(s, "."))))
            .collect(Collectors.toMap(Blindex::getKey, b -> b, (b1, b2) -> b1));
    Map<String, Blindex> current = blindexRepository.loadIndexes().stream().collect(Collectors.toMap(Blindex::getKey, b -> b));
    Map<String, Blindex> drop = new HashMap<>(current);
    create.keySet().forEach(c -> drop.remove(c));
    current.keySet().forEach(c -> create.remove(c));
    log.debug("currently indexed: " + current);
    log.debug("need to create: " + create);
    log.debug("need to remove: " + drop);
    create(create.values());
    drop(drop.values());
    blindexRepository.refreshCache();
    log.info("blindex initialization finished");
    return null;
  }


  private void create(Collection<Blindex> create) {
    List<String> errors = new ArrayList<>();
    List<Blindex> createdIndexed = new ArrayList<>();
    for (Blindex b : create) {
      log.debug("creating index on " + b.getKey());
      try {
        createdIndexed.add(blindexRepository.createIndex(b.getParamType(), b.getResourceType(), b.getPath()));
      } catch (Exception e) {
        String err = e.getMessage();
        if (e.getCause() instanceof PSQLException) {
          err = (e.getCause().getMessage().substring(0, e.getCause().getMessage().indexOf("\n")));
        }
        log.info("failed " + b.getKey() + ": " + err);
        errors.add(b.getKey() + ": " + err);
      }
    }
    log.error("failed to create " + errors.size() + " indexes");
    recalculate(createdIndexed);
  }

  private void recalculate(List<Blindex> blindexes) {
    if (CollectionUtils.isEmpty(blindexes)) {
      return;
    }
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      Map<String, List<Blindex>> resourceTypeBlindexes = blindexes.stream().collect(Collectors.groupingBy(b -> b.getResourceType()));
      log.info("recalculating " + blindexes.size() + " indexes for " + resourceTypeBlindexes.size() + " resources");
      resourceTypeBlindexes.forEach((type, indexes) -> {
        try {
          Long page = 1L;
          Integer batch = 1000;
          while (true) {
            SearchResult search = resourceSearchService.search(type, "_count", batch.toString(), "_page", page.toString());
            indexes.forEach(i -> search.getEntries().forEach(v -> blindexRepository.merge(i.getId(), v.getId(), v.getContent().getValue())));
            if (search.getEntries().size() < batch) {
              break;
            }
            page++;
          }
        } catch (Exception e) {
          log.error("failed to recalculate indexes for  " + type, e);
        }
      });
      log.info("index recalculation finished");
    });
  }

  private void drop(Collection<Blindex> drop) {
    for (Blindex b : drop) {
      try {
        blindexRepository.dropIndex(b.getParamType(), b.getResourceType(), b.getPath());
      } catch (Exception e) {
        String err = e.getCause() instanceof PSQLException ? e.getCause().getMessage() : e.getMessage();
        log.debug("failed " + b.getKey() + ": " + err);
      }
    }
  }

}
