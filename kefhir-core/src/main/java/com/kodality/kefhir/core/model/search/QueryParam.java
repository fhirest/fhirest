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
 package com.kodality.kefhir.core.model.search;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

public class QueryParam {
  private final String key;
  private final String modifier;
  private final SearchParamType type;
  private final String resourceType;

  private List<QueryParam> chains;
  private List<String> values;

  public QueryParam(String key, String modifier, SearchParamType type, String resourceType) {
    this.key = key;
    this.modifier = modifier;
    this.type = type;
    this.resourceType = resourceType;
  }

  public String getResourceType() {
    return resourceType;
  }

  public SearchParamType getType() {
    return type;
  }

  public String getKey() {
    return key;
  }

  public String getModifier() {
    return modifier;
  }

  public void addChain(QueryParam chain) {
    Validate.isTrue(values == null);
    if (chains == null) {
      chains = new ArrayList<>();
    }
    this.chains.add(chain);
  }

  public List<QueryParam> getChains() {
    return chains;
  }

  public void setValues(List<String> values) {
    if (chains != null) {
      chains.forEach(c -> c.setValues(values));
      return;
    }
    this.values = values;
  }

  public List<String> getValues() {
    Validate.isTrue(chains == null);
    return values;
  }

}
