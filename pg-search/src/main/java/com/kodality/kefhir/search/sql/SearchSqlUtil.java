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
 package com.kodality.kefhir.search.sql;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.search.QueryParam;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.search.sql.params.CompositeExpressionProvider;
import com.kodality.kefhir.search.sql.params.DateExpressionProvider;
import com.kodality.kefhir.search.sql.params.NumberExpressionProvider;
import com.kodality.kefhir.search.sql.params.QuantityExpressionProvider;
import com.kodality.kefhir.search.sql.params.ReferenceExpressionProvider;
import com.kodality.kefhir.search.sql.params.StringExpressionProvider;
import com.kodality.kefhir.search.sql.params.TokenExpressionProvider;
import com.kodality.kefhir.search.sql.params.UriExpressionProvider;
import com.kodality.kefhir.search.sql.specialparams.NothingSpecialSpecialParamsProviders.IdExpressionProvider;
import com.kodality.kefhir.search.sql.specialparams.NothingSpecialSpecialParamsProviders.LastUpdatedExpressionProvider;
import com.kodality.kefhir.search.sql.specialparams.NothingSpecialSpecialParamsProviders.NotImlementedExpressionProvider;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

public final class SearchSqlUtil {
  private static final Map<String, ExpressionProvider> specialParams;
  private static final Map<SearchParamType, ExpressionProvider> providers;

  static {
    specialParams = new HashMap<>();
    specialParams.put("_id", new IdExpressionProvider());
    specialParams.put("_lastUpdated", new LastUpdatedExpressionProvider());
    specialParams.put("_content", new NotImlementedExpressionProvider());
    specialParams.put("_text", new NotImlementedExpressionProvider());

    providers = new HashMap<>();
    providers.put(SearchParamType.STRING, new StringExpressionProvider());
    providers.put(SearchParamType.TOKEN, new TokenExpressionProvider());
    providers.put(SearchParamType.DATE, new DateExpressionProvider());
    providers.put(SearchParamType.REFERENCE, new ReferenceExpressionProvider());
    providers.put(SearchParamType.NUMBER, new NumberExpressionProvider());
    providers.put(SearchParamType.QUANTITY, new QuantityExpressionProvider());
    providers.put(SearchParamType.URI, new UriExpressionProvider());
    providers.put(SearchParamType.COMPOSITE, new CompositeExpressionProvider());
  }

  private SearchSqlUtil() {
    //
  }

  public static SqlBuilder chain(List<QueryParam> params, String alias) {
    return ReferenceExpressionProvider.chain(params, alias);
  }

  public static SqlBuilder condition(QueryParam param, String alias) {
    String key = param.getKey();
    if (specialParams.containsKey(key)) {
      return specialParams.get(key).makeExpression(param, alias);
    }
    if (!providers.containsKey(param.getType())) {
      String details = "'" + param.getType() + "' search parameter type not implemented";
      throw new FhirException(400, IssueType.NOTSUPPORTED, details);
    }
    return providers.get(param.getType()).makeExpression(param, alias);
  }

  public static SqlBuilder order(QueryParam param, String alias) {
    String value = param.getValues().get(0);
    String direction = "ASC";
    if (value.startsWith("-") || "desc".equals(param.getModifier())) {
      direction = "DESC";
      value = value.replaceFirst("-", "");
    }
    SearchParamType type = ConformanceHolder.requireSearchParam(param.getResourceType(), value).getType();
    if (specialParams.containsKey(value)) {
      return specialParams.get(value).order(param.getResourceType(), value, alias);
    }
    if (!providers.containsKey(type)) {
      String details = String.format("'%s' search parameter type not implemented", param.getType());
      throw new FhirException(400, IssueType.NOTSUPPORTED, details);
    }
    return providers.get(type).order(param.getResourceType(), value, alias).append(direction);
  }

}
