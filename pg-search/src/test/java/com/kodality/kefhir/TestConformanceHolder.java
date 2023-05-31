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

import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import java.util.HashMap;
import org.hl7.fhir.r5.model.SearchParameter;

public class TestConformanceHolder extends ConformanceHolder {

  public static void apply(SearchParameter sp) {
    searchParamGroups = searchParamGroups == null ? new HashMap<>() : searchParamGroups;
    sp.getBase().forEach(ct -> {
      searchParamGroups.putIfAbsent(ct.getValue().toCode(), new HashMap<>());
      searchParamGroups.get(ct.getValue().toCode()).put(sp.getCode(), sp);
    });
  }

}
