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
package com.kodality.kefhir.core.service.conformance;

import com.kodality.kefhir.core.exception.FhirException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CompartmentDefinition;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.ValueSet;

public class ConformanceHolder {
  protected static CapabilityStatement capabilityStatement;
  protected static List<StructureDefinition> definitions;
  //resource type -> code -> param
  protected static Map<String, Map<String, SearchParameter>> searchParamGroups;
  protected static Map<String, SearchParameter> searchParams;
  protected static List<ValueSet> valueSets;
  protected static List<CodeSystem> codeSystems;
  protected static Map<String, CompartmentDefinition> compartmentDefinitions;
  protected static Map<String, Map<String, List<String>>> compartmentDefinitionParams;

  protected static void setCapabilityStatement(CapabilityStatement capabilityStatement) {
    ConformanceHolder.capabilityStatement = capabilityStatement;
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

  public static CapabilityStatementRestResourceComponent getCapabilityResource(String type) {
    return getCapabilityStatement().getRest().stream().filter(r -> r.getMode() == RestfulCapabilityMode.SERVER)
        .map(r -> r.getResource().stream().filter(rr -> rr.getType().equals(type)).findFirst().orElse(null))
        .filter(r -> r != null)
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
      String details = type + "/" + code + " searchparam does not exist in search config";
      throw new FhirException(400, IssueType.NOTSUPPORTED, details);
    }
    return param;
  }

}
