/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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

package ee.fhir.fhirest.core.model.search;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.service.conformance.CapabilitySearchConformance;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.SearchParameter;

import static java.util.stream.Collectors.toList;

public final class SearchCriterionBuilder {
  private static final String MODIFIER = ":";
  private static final String CHAIN = ".";

  private SearchCriterionBuilder() {
    //
  }

  public static SearchCriterion parse(String query, String resourceType) {
    Map<String, List<String>> params = new HashMap<>();
    Stream.of(query.split("&")).forEach(q -> {
      String[] qr = q.split("=");
      if (qr.length != 2) {
        throw new FhirException(FhirestIssue.FEST_016, "param", q, "query", query);
      }
      params.computeIfAbsent(qr[0], (a) -> new ArrayList<>()).add(qr[1]);
    });
    return parse(params, resourceType);
  }

  public static SearchCriterion parse(Map<String, List<String>> params, String resourceType) {
    if (params == null || params.isEmpty()) {
      return new SearchCriterion(resourceType, List.of(), params);
    }
    params = new LinkedHashMap<>(params);
    params.remove("");
    params.remove(null);// well this is strange
    params.keySet().removeAll(SearchCriterion.ignoreParamKeys);

    List<QueryParam> result = new ArrayList<>();
    params.forEach((k, v) -> result.addAll(parse(k, v, resourceType)));
    return new SearchCriterion(resourceType, result, params);
  }

  public static List<QueryParam> parse(String rawKey, List<String> rawValues, String resourceType) {
    ChainForge chainsmith = buildForge(rawKey, resourceType);
    return rawValues.stream().map(value -> {
      QueryParam param = chainsmith.forge();
      param.setValues(split(value));
      return param;
    }).collect(toList());
  }

  private static List<String> split(String param) {
    return Stream.of(param.split("(?<!\\\\),")).map(s -> s.replaceAll("\\\\,", ",")).collect(toList());
  }

  private static ChainForge buildForge(String chain, String resourceType) {
    String link = StringUtils.substringBefore(chain, CHAIN);
    String key = StringUtils.substringBefore(link, MODIFIER);
    String modifier = link.contains(MODIFIER) ? StringUtils.substringAfter(link, MODIFIER) : null;

    if (SearchCriterion.resultParamKeys.contains(key)) {
      return new ChainForge(key, modifier, null, resourceType);
    }

    CapabilityStatementRestResourceSearchParamComponent conformance = CapabilitySearchConformance.get(resourceType, key);
    SearchParameter sp = ConformanceHolder.requireSearchParam(resourceType, key);
    validate(conformance, sp, key, modifier);

    ChainForge forge = new ChainForge(key, modifier, conformance.getType(), resourceType);
    if (chain.contains(CHAIN) && conformance.getType() == SearchParamType.REFERENCE) {
      String remainder = chain.contains(CHAIN) ? StringUtils.substringAfter(chain, CHAIN) : null;
      List<String> targetResourceTypes = sp.getTarget().stream().map(ct -> ct.getValue().toCode()).collect(toList());
      if (modifier != null) {
        targetResourceTypes.retainAll(Collections.singletonList(modifier));
      }
      if (targetResourceTypes.isEmpty()) {
        throw new FhirException(FhirestIssue.FEST_017, "key", key, "modifier", modifier);
      }
      targetResourceTypes.forEach(rt -> forge.nextLink(buildForge(remainder, rt)));
    }
    return forge;
  }

  private static void validate(CapabilityStatementRestResourceSearchParamComponent conformance,
                               SearchParameter sp,
                               String key,
                               String modifier) {
    if (conformance == null) {
      throw new FhirException(FhirestIssue.FEST_018, "param", key);
    }
    if (!validateModifier(conformance, sp, modifier)) {
      throw new FhirException(FhirestIssue.FEST_019, "modifier", modifier, "param", key);
    }
  }

  private static boolean validateModifier(CapabilityStatementRestResourceSearchParamComponent conformance,
                                          SearchParameter sp,
                                          String modifier) {
    if (StringUtils.isEmpty(modifier)) {
      return true;
    }
    if (conformance.getType() == SearchParamType.REFERENCE) {
      return CollectionUtils.isEmpty(sp.getTarget())
             || sp.getTarget().stream().anyMatch(t -> t.getCode().equals(modifier));
    }
    // FIXME gone from searchparameters?
    return true;
    //    return sp.getModifier().stream().anyMatch(m -> m.getValue().toCode().equals(modifier));
  }

  private static class ChainForge {
    private final String key;
    private final String modifier;
    private final SearchParamType paramType;
    private final String resourceType;

    private List<ChainForge> next;

    public ChainForge(String key, String modifier, SearchParamType paramType, String resourceType) {
      this.key = key;
      this.modifier = modifier;
      this.paramType = paramType;
      this.resourceType = resourceType;
    }

    public void nextLink(ChainForge next) {
      if (this.next == null) {
        this.next = new ArrayList<>();
      }
      this.next.add(next);
    }

    public QueryParam forge() {
      QueryParam param = new QueryParam(key, modifier, paramType, resourceType);
      if (next != null) {
        next.forEach(n -> param.addChain(n.forge()));
      }
      return param;
    }

  }
}
