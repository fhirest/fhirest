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
package com.kodality.kefhir;

import com.kodality.kefhir.core.model.search.QueryParam;
import com.kodality.kefhir.core.service.conformance.CapabilitySearchConformance;
import com.kodality.kefhir.core.service.resource.SearchUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.SearchParameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class SearchUtilTest {

  @BeforeEach
  public void mocks() {
    List<SearchParameter> searchParams = new ArrayList<>();
    CapabilityStatement capability = new CapabilityStatement();
    CapabilityStatementRestComponent rest = capability.addRest();
    rest.setMode(RestfulCapabilityMode.SERVER);

    CapabilityStatementRestResourceComponent resource = rest.addResource().setType(ResourceType.Patient.name());

    addSearchParam(resource, searchParams, "papa", SearchParamType.REFERENCE).addTarget(ResourceType.Patient.name());
    addSearchParam(resource, searchParams, "name", SearchParamType.STRING);

    TestConformanceHolder.setCapabilityStatement(capability);
    TestConformanceHolder.setSearchParams(searchParams);
    CapabilitySearchConformance.setCapability(capability);
  }

  private SearchParameter addSearchParam(CapabilityStatementRestResourceComponent resource,
                                         List<SearchParameter> searchParams,
                                         String name,
                                         SearchParamType type) {
    CapabilityStatementRestResourceSearchParamComponent conformanceSp = resource.addSearchParam();
    conformanceSp.setName(name);
    conformanceSp.setType(type);
    SearchParameter sp = new SearchParameter();
    sp.setType(type);
    sp.setName(name);
    sp.setCode(name);
    sp.addBase(resource.getType());
    searchParams.add(sp);
    return sp;
  }

  @Test
  public void chaintestNoChain() {
    List<QueryParam> q = SearchUtil.parse("name", Collections.singletonList("колян"), ResourceType.Patient.name());
    Assertions.assertEquals(q.size(), 1);
    Assertions.assertEquals(q.get(0).getValues().get(0), "колян");
  }

  @Test
  public void chaintest() {
    List<QueryParam> q = SearchUtil.parse("papa:Patient.papa:Patient.papa:Patient.papa:Patient.name:exact",
        Collections.singletonList("фарадей"),
        ResourceType.Patient.name());
    Assertions.assertEquals(q.size(), 1);
    Assertions.assertEquals(
        q.get(0)
            .getChains()
            .get(0)
            .getChains()
            .get(0)
            .getChains()
            .get(0)
            .getChains()
            .get(0)
            .getValues()
            .get(0),
        "фарадей");
  }
}
