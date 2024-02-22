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
package ee.fhir.fhirest.core.model.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

public class SearchCriterion {
  public static final String _SORT = "_sort";
  public static final String _COUNT = "_count";
  public static final String _PAGE = "_page";
  public static final String _INCLUDE = "_include";
  public static final String _REVINCLUDE = "_revinclude";
  public static final String _SUMMARY = "_summary";
  public static final String _ELEMENTS = "_elements";
  public static final String _CONTAINED = "_contained";
  public static final String _CONTAINEDTYPE = "_containedType";
  public static final List<String> ignoreParamKeys = Arrays.asList("_pretty", "_format");
  public static final List<String> resultParamKeys =
      Arrays.asList(_SORT, _COUNT, _PAGE, _INCLUDE, _REVINCLUDE, _SUMMARY, _ELEMENTS, _CONTAINED, _CONTAINEDTYPE);

  private String type;
  private List<QueryParam> chains;
  private List<QueryParam> conditions;
  private List<QueryParam> resultParams;
  private Map<String, List<String>> rawParams;

  public SearchCriterion(String type, List<QueryParam> params, Map<String, List<String>> rawParams) {
    this.type = type;
    setParams(params);
    this.rawParams = rawParams;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<QueryParam> getChains() {
    return chains;
  }

  public List<QueryParam> getConditions() {
    return conditions;
  }

  public List<QueryParam> getResultParams() {
    return resultParams;
  }

  public List<QueryParam> getResultParams(String key) {
    return resultParams.stream().filter(q -> q.getKey().equals(key)).collect(Collectors.toList());
  }

  public Map<String, List<String>> getRawParams() {
    return rawParams;
  }

  public void setParams(List<QueryParam> params) {
    chains = new ArrayList<>();
    conditions = new ArrayList<>();
    resultParams = new ArrayList<>();
    if (CollectionUtils.isEmpty(params)) {
      return;
    }
    for (QueryParam param : params) {
      if (param.getChains() != null) {
        chains.add(param);
      } else if (resultParamKeys.contains(param.getKey())) {
        resultParams.add(param);
      } else {
        conditions.add(param);
      }
    }
  }

  public Integer getCount() {
    List<QueryParam> _count = getResultParams(_COUNT);
    if (CollectionUtils.isEmpty(_count) || CollectionUtils.isEmpty(_count.get(0).getValues())) {
      return 10;
    }
    return ObjectUtils.max(0, Integer.valueOf(_count.get(0).getValues().get(0)));
  }

  public Integer getPage() {
    List<QueryParam> _page = getResultParams(_PAGE);
    if (CollectionUtils.isEmpty(_page) || CollectionUtils.isEmpty(_page.get(0).getValues())) {
      return 1;
    }
    return ObjectUtils.max(1, Integer.valueOf(_page.get(0).getValues().get(0)));
  }

}
