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

package ee.fhir.fhirest.search;

import ee.fhir.fhirest.core.model.search.SearchResult;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.core.service.resource.ResourceSearchService;
import ee.fhir.fhirest.search.index.IndexService;
import ee.fhir.fhirest.search.model.Blindex;
import ee.fhir.fhirest.search.model.StructureElement;
import ee.fhir.fhirest.search.repository.BlindexRepository;
import ee.fhir.fhirest.search.util.SearchPathUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.postgresql.util.PSQLException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindexInitializer {
  private final BlindexRepository blindexRepository;
  private final ResourceSearchService resourceSearchService;
  private final IndexService indexService;
  private final StructureDefinitionHolder structureDefinitionHolder;

  public void execute() {
    if (CollectionUtils.isEmpty(ConformanceHolder.getDefinitions()) || ConformanceHolder.getCapabilityStatement() == null) {
      log.error("blindex: will not run. conformance not yet initialized");
      return;
    }

    log.info("refreshing search indexes...");
    List<SearchParameter> searchParameters = findCapabilityDefinedParameters();
    Map<String, Blindex> create =
        searchParameters.stream()
            .filter(sp -> sp.getExpression() != null && sp.getType() != SearchParamType.COMPOSITE)
            .flatMap(sp -> SearchPathUtil.parsePaths(sp.getExpression()).stream()
                .map(s -> new Blindex(sp.getType().toCode(), StringUtils.substringBefore(s, "."), StringUtils.substringAfter(s, "."))))
            .collect(Collectors.toMap(Blindex::getKey, b -> b, (b1, b2) -> b1));
    Map<String, Blindex> current = blindexRepository.loadIndexes().stream().collect(Collectors.toMap(Blindex::getKey, b -> b));
    Map<String, Blindex> drop = new HashMap<>(current);
    create.keySet().forEach(drop::remove);
    current.keySet().forEach(create::remove);
    log.debug("currently indexed: {}", current.keySet());
    log.debug("need to create: {}", create.keySet());
    log.debug("need to remove: {}", drop.keySet());
    create(create.values());
    drop(drop.values());
    blindexRepository.refreshCache();
    log.info("blindex initialization finished");
  }

  private List<SearchParameter> findCapabilityDefinedParameters() {
    List<String> resourceTypes = List.of("http://hl7.org/fhir/StructureDefinition/DomainResource", "http://hl7.org/fhir/StructureDefinition/Resource");
    List<String> defined = ConformanceHolder.getDefinitions().stream()
        .filter(def -> def.getBaseDefinition() != null && resourceTypes.contains(def.getBaseDefinition()))
        .map(StructureDefinition::getName).toList();
    Map<String, SearchParameter> spDefinitions = ConformanceHolder.getSearchParams().values().stream().collect(Collectors.toMap(d -> d.getUrl(), d -> d));

    return ConformanceHolder.getCapabilityStatement().getRest().stream().filter(r -> r.getMode() == RestfulCapabilityMode.SERVER).flatMap(rest -> {
      return rest.getResource().stream().filter(res -> defined.contains(res.getType())).flatMap(res -> {
        return res.getSearchParam().stream().map(sp -> {
          if (!spDefinitions.containsKey(sp.getDefinition())) {
            log.error("could not init index: " + res.getType() + "." + sp.getName() + "@" + sp.getDefinition() + ", definition of search parameter not found");
            return null;
          }
          return spDefinitions.get(sp.getDefinition());
        }).filter(Objects::nonNull);
      });
    }).collect(Collectors.toList());
  }


  private void create(Collection<Blindex> create) {
    List<String> errors = new ArrayList<>();
    List<Blindex> createdIndexed = new ArrayList<>();
    create.forEach(b -> {
      log.debug("creating index on " + b.getKey());
      try {
        if (!structureDefinitionHolder.getStructureElements().containsKey(b.getResourceType())) {
          log.debug("failed {}: unknown resource type: {}", b.getKey(), b.getResourceType());
          errors.add(b.getKey() + ": unknown resource type: " + b.getResourceType());
          return;
        }
        Map<String, List<StructureElement>> elements = structureDefinitionHolder.getStructureElements().get(b.getResourceType());
        if (!elements.containsKey(b.getPath())) {
          log.debug("failed {} - {}: unknown yet", b.getResourceType(), b.getKey());
          errors.add(b.getKey() + ": unknown yet");
          return;
        }
        if (elements.get(b.getPath()).stream().anyMatch(el -> {
          return List.of("canonical", "Money", "url", "Address", "BackboneElement", "Duration").contains(el.getType())
                 || (b.getParamType().equalsIgnoreCase("reference") && el.getType().equals("uri")); //TODO
        })) {
          log.debug("failed {} - {}: not configured", b.getResourceType(), b.getKey());
          errors.add(b.getKey() + ": " + " not configured");
          return;
        }
        createdIndexed.add(blindexRepository.createIndex(b.getParamType(), b.getResourceType(), b.getPath()));
      } catch (Exception e) {
        String err = e.getMessage();
        if (e.getCause() instanceof PSQLException) {
          err = (e.getCause().getMessage().substring(0, e.getCause().getMessage().indexOf("\n")));
        }
        log.info("failed {} - {}: {}", b.getResourceType(), b.getKey(), err);
        errors.add(b.getKey() + ": " + err);
      }
    });
    if (!errors.isEmpty()) {
      log.info("failed to create " + errors.size() + " indexes");
    }
    if (!createdIndexed.isEmpty()) {
      log.info("created " + createdIndexed.size() + " indexes");
    }
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
            indexes.forEach(i -> search.getEntries().forEach(v -> indexService.saveIndex(v, i)));
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
