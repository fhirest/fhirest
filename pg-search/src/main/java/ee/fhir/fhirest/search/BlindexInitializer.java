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
    // Map blindex-key → SearchParameter.code so we can label blindex.index_name with the query param code 
    // (e.g. "name", "date")
    Map<String, String> codeByKey = new HashMap<>();

    Map<String, Blindex> create =
        searchParameters.stream()
            .filter(sp -> sp.getExpression() != null && sp.getType() != SearchParamType.COMPOSITE)
            .flatMap(sp -> SearchPathUtil.parsePaths(sp.getExpression()).stream()
                .map(s -> {
                  Blindex b = new Blindex(sp.getType().toCode(),
                                          StringUtils.substringBefore(s, "."),
                                          StringUtils.substringAfter(s, "."));
                  codeByKey.put(b.getKey(), sp.getCode());
                  return b;
                }))
            .collect(Collectors.toMap(Blindex::getKey, b -> b, (b1, b2) -> b1));
    Map<String, Blindex> current = blindexRepository.loadIndexes().stream().collect(Collectors.toMap(Blindex::getKey, b -> b));
    Map<String, Blindex> drop = new HashMap<>(current);
    create.keySet().forEach(drop::remove);
    current.keySet().forEach(create::remove);
    log.debug("currently indexed: {}", current.keySet());
    log.debug("need to create: {}", create.keySet());
    log.debug("need to remove: {}", drop.keySet());
    create(create.values(), codeByKey);
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


  private void create(Collection<Blindex> create, Map<String,String> codeByKey) {
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
        String originalPath = b.getPath();
        
        if (!elements.containsKey(originalPath)) {
          // Build candidate variants of the FHIRPath expression to handle differences
          // between SearchParameter expressions and StructureDefinition element keys.
          // Strip away any .where(...) predicates to get the "raw" path
          String stripped = originalPath.replaceAll("\\.where\\(.*?\\)", "");

          // Ensure path is prefixed with the resource type, e.g. "Measure.relatedArtifact"
          String prefixed = b.getResourceType() + "." + stripped;

          // Remove the resource type prefix, in case elements are keyed without it
          String dePrefixed = StringUtils.removeStart(stripped, b.getResourceType() + ".");

          // Handle polymorphic "value[x]" cases:
          // Replace .valueReference with .value (may appear in StructureDefinitions this way)
          String poly1 = stripped.replace(".valueReference", ".value");
          String poly2 = prefixed.replace(".valueReference", ".value");
          String poly3 = dePrefixed.replace(".valueReference", ".value");
        
          // Collect all candidate variants to test
          List<String> candidates = new ArrayList<>();
          candidates.add(stripped);
          candidates.add(prefixed);
          candidates.add(dePrefixed);
          candidates.add(poly1);
          candidates.add(poly2);
          candidates.add(poly3);
        
          // Try to find the first variant that matches the known StructureDefinition elements
          String matched = null;
          for (String c : candidates) {
            if (elements.containsKey(c)) { matched = c; break; }
          }

          // Last resort: scan entries where the child list contains the Reference we want
          if (matched == null) {
            for (Map.Entry<String, List<StructureElement>> e : elements.entrySet()) {
              boolean hit = e.getValue().stream().anyMatch(se -> {
                String t = se.getType();
              
                // --- Reference search parameter ---
                // A reference-type SP (paramType=reference) can be satisfied by:
                // - "Reference" (normal FHIR reference)
                if ("reference".equalsIgnoreCase(b.getParamType())) {
                  return "Reference".equals(t);  // spec-accurate: references only
                }
              
                // --- Token search parameter ---
                // Token-type SPs can be backed by:
                // - "Coding" (most direct carrier of system+code)
                // - "CodeableConcept" (wraps one or more Coding, sometimes only text)
                // - "Identifier" (system+value behave like token)
                // - "ContactPoint" (system+value also treated like token)
                // - "boolean" and "string" (FHIR spec allows indexing of simple booleans/strings as token codes)
                if ("token".equalsIgnoreCase(b.getParamType())) {
                  return List.of("Coding", "CodeableConcept", "Identifier", "ContactPoint", "boolean", "string").contains(t);
                }
              
                // --- String search parameter ---
                // String-type SPs may be satisfied by any primitive string-like element:
                // - "string", "markdown", "id", "code" (all map to string search behavior)
                // - "uri" and "canonical" (sometimes stored as strings, treated like text for searching)
                // - these may be "references" which are not of type Reference
                if ("string".equalsIgnoreCase(b.getParamType())) {
                  return List.of("string", "markdown", "id", "code", "uri", "canonical").contains(t);
                }
              
                // --- URI search parameter ---
                // URI-type SPs specifically accept:
                // - "uri" (native)
                // - "canonical" (valid per spec; considered URI under the hood)
                if ("uri".equalsIgnoreCase(b.getParamType())) {
                  return List.of("uri", "canonical").contains(t);
                }
              
                // --- Quantity search parameter ---
                // Quantity-type SPs must be real quantities:
                // - "Quantity" (has value, unit, system, code)
                if ("quantity".equalsIgnoreCase(b.getParamType())) {
                  return "Quantity".equals(t);
                }
              
                // --- Date search parameter ---
                // Date-type SPs are satisfied by date-like primitives and complex Periods:
                // - "date", "dateTime", "instant" (point-in-time types)
                // - "Period" (start/end ranges)
                if ("date".equalsIgnoreCase(b.getParamType())) {
                  return List.of("date", "dateTime", "instant", "Period").contains(t);
                }
              
                // --- Number search parameter ---
                // Number-type SPs accept numeric primitives:
                // - "decimal", "integer", "unsignedInt", "positiveInt"
                if ("number".equalsIgnoreCase(b.getParamType())) {
                  return List.of("decimal", "integer", "unsignedInt", "positiveInt").contains(t);
                }
              
                // Fallback: nothing matched
                return false;
              });
              if (hit) {
                matched = e.getKey(); 
                break;
              }
            }
          }
        
          if (matched == null) {
            log.info("failed {} - {}: unknown yet", b.getResourceType(), b.getKey());
            errors.add(b.getKey() + ": unknown yet");
            return;
          }
        
          // Use the matched structural key for type checks; still store the ORIGINAL path in blindex
          if (elements.get(matched).stream().anyMatch(el -> {
            return List.of("Money", "url", "Address", "BackboneElement", "Duration").contains(el.getType());
          })) {
            log.debug("failed {} - {}: not configured", b.getResourceType(), b.getKey());
            errors.add(b.getKey() + ": not configured");
            return;
          }

          // infer effective type and override if needed
          String effectiveParamType = inferEffectiveParamType(b, elements, matched);
          if (!Objects.equals(effectiveParamType, b.getParamType())) {
            log.warn("blindex: {} -> overriding paramType {} → {} (path={}, leafTypes={})",
                b.getKey(), b.getParamType(), effectiveParamType, matched,
                elements.get(matched).stream().map(StructureElement::getType).collect(Collectors.toSet()));
          }

          createdIndexed.add(blindexRepository.createIndex(effectiveParamType, 
              b.getResourceType(), originalPath, codeByKey.get(b.getKey()))); 
          return;
        }

        // Normal path (exact key already exists)
        if (elements.get(originalPath).stream().anyMatch(el -> {
          return List.of("Money", "url", "Address", "BackboneElement", "Duration").contains(el.getType());
        })) {
          log.debug("failed {} - {}: not configured", b.getResourceType(), b.getKey());
          errors.add(b.getKey() + ":  not configured");
          return;
        }

        // infer effective type with the exact key
        String effectiveParamType = inferEffectiveParamType(b, elements, originalPath);
        if (!Objects.equals(effectiveParamType, b.getParamType())) {
          log.warn("blindex: {} -> overriding paramType {} → {} (path={}, leafTypes={})",
              b.getKey(), b.getParamType(), effectiveParamType, originalPath,
              elements.get(originalPath).stream().map(StructureElement::getType).collect(Collectors.toSet()));
        }

        createdIndexed.add(blindexRepository.createIndex(effectiveParamType, b.getResourceType(), originalPath,
            codeByKey.get(b.getKey())));
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
            if (search.getEntries().size() < batch) break;
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

  private String inferEffectiveParamType(Blindex b, Map<String, List<StructureElement>> elements, String matchedPath) {
    String declaredParamType = b.getParamType();
    List<StructureElement> leafs = elements.get(matchedPath);
    if (leafs == null || leafs.isEmpty()) return declaredParamType;

    var leafTypes = leafs.stream().map(StructureElement::getType).collect(Collectors.toSet());

    // if search parameter declares "reference" but this path ends at canonical ".resource" (not ".resourceReference"):
    // no actual Reference node present → flip to URI so we index under base_index_uri and apply URI semantics.
    if ("reference".equalsIgnoreCase(declaredParamType)) {
      boolean hasReference = leafTypes.contains("Reference");
      boolean hasCanonicalOrUri = leafTypes.contains("canonical") || leafTypes.contains("uri") || leafTypes.contains("url");
      boolean isResourceReferencePath = matchedPath.endsWith(".resourceReference");
      boolean isResourceCanonicalPath  = matchedPath.endsWith(".resource");
      if (!hasReference && hasCanonicalOrUri && (isResourceCanonicalPath && !isResourceReferencePath)) {
        return "uri";
      }
    }

    // If search parameter declares "string" but the terminal node is canonical|uri|url,
    // flip to URI so we index in base_index_uri and apply URI semantics (:below/:above, exact)
    if ("string".equalsIgnoreCase(declaredParamType)) {
      boolean hasCanonicalOrUri = leafTypes.contains("canonical") || leafTypes.contains("uri") || leafTypes.contains("url");
      if (hasCanonicalOrUri) return "uri";
    }

    return declaredParamType;
  }
}
