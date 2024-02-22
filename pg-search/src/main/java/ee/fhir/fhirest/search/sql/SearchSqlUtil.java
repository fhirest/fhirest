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
 package ee.fhir.fhirest.search.sql;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.search.sql.params.CompositeExpressionProvider;
import ee.fhir.fhirest.search.sql.params.DateExpressionProvider;
import ee.fhir.fhirest.search.sql.params.NumberExpressionProvider;
import ee.fhir.fhirest.search.sql.params.QuantityExpressionProvider;
import ee.fhir.fhirest.search.sql.params.ReferenceExpressionProvider;
import ee.fhir.fhirest.search.sql.params.StringExpressionProvider;
import ee.fhir.fhirest.search.sql.params.TokenExpressionProvider;
import ee.fhir.fhirest.search.sql.params.UriExpressionProvider;
import ee.fhir.fhirest.search.sql.specialparams.NothingSpecialSpecialParamsProviders.IdExpressionProvider;
import ee.fhir.fhirest.search.sql.specialparams.NothingSpecialSpecialParamsProviders.LastUpdatedExpressionProvider;
import ee.fhir.fhirest.search.sql.specialparams.NothingSpecialSpecialParamsProviders.NotImlementedExpressionProvider;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

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
      return specialParams.get(value).order(param.getResourceType(), value, alias, direction);
    }
    if (!providers.containsKey(type)) {
      String details = String.format("'%s' search parameter type not implemented", param.getType());
      throw new FhirException(400, IssueType.NOTSUPPORTED, details);
    }
    return providers.get(type).order(param.getResourceType(), value, alias, direction).append(direction);
  }

}
