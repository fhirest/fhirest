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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;

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
