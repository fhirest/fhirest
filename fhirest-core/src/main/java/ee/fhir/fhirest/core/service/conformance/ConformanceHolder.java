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

package ee.fhir.fhirest.core.service.conformance;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CompartmentDefinition;
import org.hl7.fhir.r5.model.OperationDefinition;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.TerminologyCapabilities;
import org.hl7.fhir.r5.model.ValueSet;

public class ConformanceHolder {
  protected static CapabilityStatement capabilityStatement;
  protected static TerminologyCapabilities terminologyCapabilities;
  protected static List<StructureDefinition> definitions;
  //resource type -> code -> param
  protected static Map<String, Map<String, SearchParameter>> searchParamGroups;
  protected static Map<String, SearchParameter> searchParams;
  protected static List<ValueSet> valueSets;
  protected static List<CodeSystem> codeSystems;
  protected static Map<String, CompartmentDefinition> compartmentDefinitions;
  protected static Map<String, Map<String, List<String>>> compartmentDefinitionParams;
  protected static Map<String, OperationDefinition> operationDefinitions;

  protected static void setCapabilityStatement(CapabilityStatement capabilityStatement) {
    ConformanceHolder.capabilityStatement = capabilityStatement;
  }

  protected static void setTerminologyCapabilities(TerminologyCapabilities terminologyCapabilities) {
    ConformanceHolder.terminologyCapabilities = terminologyCapabilities;
  }

  protected static void setStructureDefinitions(List<StructureDefinition> definitions) {
    ConformanceHolder.definitions = new ArrayList<>(definitions);
  }

  protected static void setSearchParamGroups(List<SearchParameter> searchParamGroups) {
    ConformanceHolder.searchParamGroups = new HashMap<>();
    ConformanceHolder.searchParams = new HashMap<>();
    searchParamGroups.forEach(p -> {
      p.getBase().forEach(ct -> ConformanceHolder.searchParamGroups.computeIfAbsent(ct.getValue().toCode(), x -> new HashMap<>()).put(p.getCode(), p));
      searchParams.put(p.getId(), p);
    });
  }

  protected static void setValueSets(List<ValueSet> valueSets) {
    ConformanceHolder.valueSets = valueSets;
  }

  protected static void setCodeSystems(List<CodeSystem> codeSystems) {
    ConformanceHolder.codeSystems = codeSystems;
  }

  protected static void setOperationDefinitions(List<OperationDefinition> operationDefinitions) {
    ConformanceHolder.operationDefinitions = operationDefinitions == null ? null
        : operationDefinitions.stream().collect(Collectors.toMap(s -> s.getUrl(), s -> s));
  }

  protected static void setCompartmentDefinitions(List<CompartmentDefinition> compartmentDefinitions) {
    ConformanceHolder.compartmentDefinitions = new HashMap<>();
    ConformanceHolder.compartmentDefinitionParams = new HashMap<>();
    compartmentDefinitions.forEach(cd -> {
      String base = cd.getCode().toCode();
      ConformanceHolder.compartmentDefinitions.put(base, cd);
      ConformanceHolder.compartmentDefinitionParams.put(base, new HashMap<>());
      cd.getResource().stream().filter(r -> CollectionUtils.isNotEmpty(r.getParam())).forEach(cr -> {
        List<String> params = cr.getParam().stream().map(s -> s.getValue()).collect(Collectors.toList());
        compartmentDefinitionParams.get(base).put(cr.getCode(), params);
      });
    });
  }

  public static CapabilityStatement getCapabilityStatement() {
    return capabilityStatement;
  }

  public static TerminologyCapabilities getTerminologyCapabilities() {
    return terminologyCapabilities;
  }

  public static CapabilityStatementRestResourceComponent getCapabilityResource(String type) {
    return getCapabilityStatement().getRest().stream().filter(r -> r.getMode() == RestfulCapabilityMode.SERVER)
        .map(r -> r.getResource().stream().filter(rr -> rr.getType().equals(type)).findFirst().orElse(null))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  public static List<StructureDefinition> getDefinitions() {
    return definitions == null ? new ArrayList<>() : new ArrayList<>(definitions);
  }

  public static List<ValueSet> getValueSets() {
    return valueSets;
  }

  public static List<CodeSystem> getCodeSystems() {
    return codeSystems;
  }

  public static CompartmentDefinition getCompartmentDefinition(String resourceType) {
    return compartmentDefinitions.get(resourceType);
  }

  public static List<String> getCompartmentParam(String resourceType, String compartment) {
    return compartmentDefinitionParams.containsKey(resourceType) ? compartmentDefinitionParams.get(resourceType).get(compartment) : null;
  }

  public static Map<String, SearchParameter> getSearchParams() {
    return searchParams;
  }

  public static List<SearchParameter> getSearchParams(String type) {
    if (!searchParamGroups.containsKey(type)) {
      return null;
    }
    return new ArrayList<>(searchParamGroups.get(type).values());
  }

  public static SearchParameter getSearchParam(String type, String code) {
    if (!searchParamGroups.containsKey(type) || !searchParamGroups.get(type).containsKey(code)) {
      if ("Resource".equals(type)) {
        return null;
      }
      return getSearchParam("Resource", code); // try root?
    }
    return searchParamGroups.get(type).get(code);
  }

  public static SearchParameter requireSearchParam(String type, String code) {
    SearchParameter param = getSearchParam(type, code);
    if (param == null) {
      throw new FhirException(FhirestIssue.FEST_024, "param", code, "resource", type);
    }
    return param;
  }

  public static List<OperationDefinition> getOperationDefinitions() {
    return operationDefinitions == null ? List.of() : new ArrayList<>(operationDefinitions.values());
  }

  public static OperationDefinition getOperationDefinition(String url) {
    return operationDefinitions == null ? null : operationDefinitions.get(url);
  }

}
