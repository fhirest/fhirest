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
import ee.fhir.fhirest.core.service.FhirPath;
import ee.fhir.fhirest.core.util.JsonUtil;
import ee.fhir.fhirest.search.StructureDefinitionHolder;
import ee.fhir.fhirest.search.model.Blindex;
import ee.fhir.fhirest.search.model.StructureElement;
import ee.fhir.fhirest.search.repository.BlindexRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Identifier;
import org.hl7.fhir.r5.model.Quantity;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;

@Component
public class IndexService {
  private final SearchIndexRepository searchIndexRepository;
  private final Map<String, TypeIndexRepository> indexRepos;
  private final StructureDefinitionHolder structureDefinitionHolder;
  private final FhirPath fhirPath;
  private static final FhirContext CTX = FhirContext.forR5();
  private static final IParser PARSER = CTX.newJsonParser().setPrettyPrint(false);

  public IndexService(SearchIndexRepository searchIndexRepository,
      List<TypeIndexRepository> repos, 
      StructureDefinitionHolder structureDefinitionHolder,
      FhirPath fhirPath) {
    this.searchIndexRepository = searchIndexRepository;
    this.indexRepos = repos.stream().collect(Collectors.toMap(TypeIndexRepository::getType, r -> r));
    this.structureDefinitionHolder = structureDefinitionHolder;
    this.fhirPath = fhirPath;
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

      Map<String, List<StructureElement>> map = structureDefinitionHolder.getStructureElements().get(blindex.getResourceType());
      List<StructureElement> elements = map != null ? map.get(blindex.getPath()) : null;

      if (version == null || version.getContent() == null) {
        // version deleted: index nothing (empty values).
        List<T> values = (elements == null) ? List.of()
            : elements.stream()
                .flatMap(el -> JsonUtil.fhirpathSimple(null, el.getChild())    // jsonObject is null
                    .flatMap(obj -> repo.map(obj, el.getType()))
                    .filter(Objects::nonNull))
                .collect(toList());
        repo.save(sid, version, blindex, values);
        return;
      }

      // decide if we should use FHIRPath evaluation
      boolean hasPredicates = blindex.getPath().contains(".where(") || blindex.getPath().contains(".exists()") || 
          blindex.getPath().contains("!=") || blindex.getPath().contains("=") || blindex.getPath().contains("<") || blindex.getPath().contains(">");

      
      if (elements == null || hasPredicates) {
        // Build full FHIRPath expression (ensure it starts with ResourceType.)
        String fullExpr = blindex.getPath().startsWith(blindex.getResourceType() + ".")
            ? blindex.getPath()
            : blindex.getResourceType() + "." + blindex.getPath();
        
        // Normalize structure-field names to FHIRPath choice syntax
        String evalExpr = fullExpr
            .replace(".valueReference", ".value.ofType(Reference)")
            .replace(".valueCoding", ".value.ofType(Coding)")
            .replace(".valueString", ".value.ofType(string)")
            .replace(".valueUri", ".value.ofType(uri)")
            .replace(".valueBoolean", ".value.ofType(boolean)")
            .replace(".valueQuantity", ".value.ofType(Quantity)");
        
        // Evaluate against the resource JSON
        List<?> hits = fhirPath.evaluate(version.getContent().getValue(), evalExpr);

        List<T> values = switch (blindex.getParamType().toLowerCase()) {
          case "reference" -> adaptReferences(hits).stream()
              .flatMap(obj -> repo.map(obj, "Reference"))
              .filter(Objects::nonNull)
              .collect(toList());

          case "token" -> adaptTokens(hits).stream()
              // Let your TokenIndexRepository.map decide by type hint; "Coding" is a safe default
              .flatMap(obj -> repo.map(obj, "Coding"))
              .filter(Objects::nonNull)
              .collect(toList());

          case "string" -> {
              List<Map<String,Object>> ss;
              if (blindex.getPath().contains("location-boundary-geojson")) {
                ss = adaptSpecialAsString(hits);
              } else {
                ss = adaptStrings(hits);
              }
              yield ss.stream()
                  .flatMap(obj -> repo.map(obj, "string"))
                  .filter(Objects::nonNull)
                  .collect(toList());
            }

            case "uri" -> adaptUris(hits).stream()
                .flatMap(obj -> repo.map(obj, "uri"))
                .filter(Objects::nonNull)
                .collect(toList());
        
            case "quantity" -> adaptQuantities(hits).stream()
                .flatMap(obj -> repo.map(obj, "Quantity"))
                .filter(Objects::nonNull)
                .collect(toList());

            case "date", "number" -> {
              // If you have dedicated repos, add adapters similar to Quantity/String
              // For now, fall back to structural path if present; otherwise index nothing
              if (elements == null) yield List.<T>of();
              yield elements.stream().flatMap(el ->
                  JsonUtil.fhirpathSimple(jsonObject, el.getChild())
                      .flatMap(obj -> repo.map(obj, el.getType()))
                      .filter(Objects::nonNull)
              ).collect(toList());
            }

            default -> {
              // Unknown param type; fall back to structural if available
              if (elements == null) yield List.<T>of();
              yield elements.stream().flatMap(el ->
                  JsonUtil.fhirpathSimple(jsonObject, el.getChild())
                      .flatMap(obj -> repo.map(obj, el.getType()))
                      .filter(Objects::nonNull)
              ).collect(toList());
            }
          };

          repo.save(sid, version, blindex, values);
          return;
       }
    });
  }
  
  private static List<Map<String, Object>> adaptReferences(List<?> hits) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object o : hits) {
      if (o instanceof Reference ref && ref.hasReference()) {
        out.add(Map.of("reference", ref.getReference()));
      } else if (o instanceof UriType uri && uri.hasValue()) {
        // Some .resource/canonical cases may surface as UriType
        out.add(Map.of("reference", uri.getValue()));
      } else if (o instanceof StringType st && st.hasValue()) {
        // If FHIRPath gives a plain string "Patient/123"
        out.add(Map.of("reference", st.getValue()));
      }
    }
    return out;
  }

  private static List<Map<String, Object>> adaptTokens(List<?> hits) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object o : hits) {
      if (o instanceof Coding c && c.hasCode()) {
        Map<String, Object> m = new HashMap<>();
        if (c.hasSystem()) m.put("system", c.getSystem());
        m.put("code", c.getCode());
        if (c.hasDisplay()) m.put("display", c.getDisplay());
        out.add(m);
      } else if (o instanceof CodeableConcept cc) {
        // Prefer first Coding; if none, fall back to text as a pseudo-code
        if (cc.hasCoding()) {
          Coding c = cc.getCodingFirstRep();
          if (c.hasCode() || c.hasSystem()) {
            Map<String, Object> m = new HashMap<>();
            if (c.hasSystem()) m.put("system", c.getSystem());
            if (c.hasCode())   m.put("code", c.getCode());
            if (c.hasDisplay()) m.put("display", c.getDisplay());
            out.add(m);
          }
        } else if (cc.hasText()) {
          out.add(Map.of("code", cc.getText()));
        }
      } else if (o instanceof Identifier id && id.hasValue()) {
        Map<String, Object> m = new HashMap<>();
        if (id.hasSystem()) m.put("system", id.getSystem());
        m.put("code", id.getValue());
        out.add(m);
      } else if (o instanceof ContactPoint cp && cp.hasValue()) {
        Map<String, Object> m = new HashMap<>();
        if (cp.hasSystem()) m.put("system", cp.getSystem().toCode());
        m.put("code", cp.getValue());
        out.add(m);
      } else if (o instanceof BooleanType bt) {
        out.add(Map.of("code", bt.booleanValue() ? "true" : "false"));
      } else if (o instanceof StringType st && st.hasValue()) {
        out.add(Map.of("code", st.getValue()));
      }
    }
    return out;
  }

  private static List<Map<String, Object>> adaptSpecialAsString(List<?> hits) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object o : hits) {
      if (o instanceof Base b) {
        String v = b.primitiveValue();
        if (v == null) v = toJsonString(b);
        if (v != null) out.add(Map.of("value", v));
      }
    }
    return out;
  }

  private static String toJsonString(Base b) {
    if (b == null) return null;
    try {
      return PARSER.encodeToString((IBase) b);
    } catch (Exception e) {
      String pv = b.primitiveValue();
      return pv != null ? pv : String.valueOf(b);
    }
  }

  private static List<Map<String, Object>> adaptStrings(List<?> hits) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object o : hits) {
      if (o instanceof Base b) {
        String v = b.primitiveValue();
        if (v != null) out.add(Map.of("value", v));
      }
    }
    return out;
  }

  private static List<Map<String, Object>> adaptUris(List<?> hits) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object o : hits) {
      if (o instanceof Base b) {
        String v = b.primitiveValue();
        if (v != null) out.add(Map.of("uri", v));
      }
    }
    return out;
  }

  private static List<Map<String, Object>> adaptQuantities(List<?> hits) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object o : hits) {
      if (o instanceof Quantity q && q.hasValue()) {
        Map<String, Object> m = new HashMap<>();
        m.put("value", q.getValue());
        if (q.hasUnit()) m.put("unit", q.getUnit());
        if (q.hasSystem()) m.put("system", q.getSystem());
        if (q.hasCode()) m.put("code", q.getCode());
        out.add(m);
      }
    }
    return out;
  }

  public void deleteResource(ResourceId id) {
    Long sid = searchIndexRepository.deleteResource(id);
    if (sid != null) {
      saveIndexes(sid, null, BlindexRepository.getIndexes(id.getResourceType()));
    }
  }

}