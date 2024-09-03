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

package ee.fhir.fhirest;

import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.core.model.search.SearchCriterionBuilder;
import ee.fhir.fhirest.core.service.conformance.CapabilitySearchConformance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.Enumerations.VersionIndependentResourceTypesAll;
import org.hl7.fhir.r5.model.ResourceType;
import org.hl7.fhir.r5.model.SearchParameter;
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

    addSearchParam(resource, searchParams, "papa", SearchParamType.REFERENCE).addTarget(VersionIndependentResourceTypesAll.PATIENT);
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
    sp.addBase(VersionIndependentResourceTypesAll.fromCode(resource.getType()));
    searchParams.add(sp);
    return sp;
  }

  @Test
  public void chaintestNoChain() {
    List<QueryParam> q = SearchCriterionBuilder.parse("name", Collections.singletonList("колян"), ResourceType.Patient.name());
    Assertions.assertEquals(q.size(), 1);
    Assertions.assertEquals(q.get(0).getValues().get(0), "колян");
  }

  @Test
  public void chaintest() {
    List<QueryParam> q = SearchCriterionBuilder.parse("papa:Patient.papa:Patient.papa:Patient.papa:Patient.name:exact",
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
