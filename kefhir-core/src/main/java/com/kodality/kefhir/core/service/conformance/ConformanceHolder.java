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
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;

import static java.util.stream.Collectors.toList;

public class ConformanceHolder {
  protected static CapabilityStatement capabilityStatement;
  protected static Map<String, StructureDefinition> definitions;
  //resource type -> code -> param
  protected static Map<String, Map<String, SearchParameter>> searchParams;
  protected static List<ValueSet> valueSets;
  protected static List<CodeSystem> codeSystems;

  protected static void setCapabilityStatement(CapabilityStatement capabilityStatement) {
    ConformanceHolder.capabilityStatement = capabilityStatement;
  }

  protected static void setStructureDefinitions(List<StructureDefinition> definitions) {
    ConformanceHolder.definitions = new HashMap<>();
    definitions.forEach(d -> ConformanceHolder.definitions.put(d.getName(), d));
  }

  protected static void setSearchParams(List<SearchParameter> searchParams) {
    ConformanceHolder.searchParams = new HashMap<>();
    searchParams.forEach(p -> p.getBase().forEach(
        ct -> ConformanceHolder.searchParams.computeIfAbsent(ct.getValue(), x -> new HashMap<>()).put(p.getCode(), p)));
  }

  protected static void setValueSets(List<ValueSet> valueSets) {
    ConformanceHolder.valueSets = valueSets;
  }

  protected static void setCodeSystems(List<CodeSystem> codeSystems) {
    ConformanceHolder.codeSystems = codeSystems;
  }

  public static CapabilityStatement getCapabilityStatement() {
    return capabilityStatement;
  }

  public static List<StructureDefinition> getDefinitions() {
    return new ArrayList<>(definitions.values());
  }

  public static StructureDefinition getDefinition(String type) {
    return definitions.get(type);
  }

  public static List<ValueSet> getValueSets() {
    return valueSets;
  }

  public static List<CodeSystem> getCodeSystems() {
    return codeSystems;
  }

  public static List<SearchParameter> getSearchParams() {
    return searchParams.values().stream().flatMap(m -> m.values().stream()).collect(toList());
  }

  public static List<SearchParameter> getSearchParams(String type) {
    if (!searchParams.containsKey(type)) {
      return null;
    }
    return new ArrayList<>(searchParams.get(type).values());
  }

  public static SearchParameter getSearchParam(String type, String code) {
    if (!searchParams.containsKey(type) || !searchParams.get(type).containsKey(code)) {
      if ("Resource".equals(type)) {
        return null;
      }
      return getSearchParam("Resource", code); // try root?
    }
    return searchParams.get(type).get(code);
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
