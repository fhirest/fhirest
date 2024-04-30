/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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
  private final Map<String, List<String>> rawParams;

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
