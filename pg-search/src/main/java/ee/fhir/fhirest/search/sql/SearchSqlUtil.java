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

package ee.fhir.fhirest.search.sql;

import ee.fhir.fhirest.core.exception.FhirServerException;
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
import java.util.Set;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;

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
      throw new FhirServerException("'" + param.getType() + "' search parameter type not implemented");
    }
    return providers.get(param.getType()).makeExpression(param, alias);
  }

  /** Force-build using a specific SearchParamType (e.g., URI or REFERENCE) without mutating the original. */
  public static SqlBuilder conditionAsType(QueryParam param, SearchParamType forcedType, String alias) {
    if (param.getType() == forcedType) {
      return condition(param, alias);
    }
    QueryParam copy = copyWithType(param, forcedType);
    return condition(copy, alias);
  }

  /** Combine limited-to types. If both URI and REFERENCE requested, returns "(uriExpr OR refExpr)". */
  public static SqlBuilder conditionLimitedTo(QueryParam param, String alias, Set<SearchParamType> allowedTypes) {
    if (allowedTypes == null || allowedTypes.isEmpty()) {
      return condition(param, alias);
    }
    boolean wantUri = allowedTypes.contains(SearchParamType.URI);
    boolean wantRef = allowedTypes.contains(SearchParamType.REFERENCE);
    SqlBuilder uriExpr = wantUri ? conditionAsType(param, SearchParamType.URI, alias) : null;
    SqlBuilder refExpr = wantRef ? conditionAsType(param, SearchParamType.REFERENCE, alias) : null;

    if (uriExpr != null && refExpr != null) {
      return new SqlBuilder().append("(").append(uriExpr).append(" OR ").append(refExpr).append(")");
    }
    return uriExpr != null ? uriExpr : refExpr;
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
      throw new FhirServerException(String.format("'%s' search parameter type not implemented", param.getType()));

    }
    return providers.get(type).order(param.getResourceType(), value, alias, direction).append(direction);
  }

  // ---- helper: shallow copy with a different SearchParamType ----
  private static QueryParam copyWithType(QueryParam p, SearchParamType newType) {
    if (p.getType() == newType) return p;
    QueryParam q = new QueryParam(p.getKey(), p.getModifier(), newType, p.getResourceType());
    if (p.getChains() != null) {
      for (QueryParam c : p.getChains()) {
        q.addChain(c);
      }
    }
    if (p.getValues() != null) {
      q.setValues(p.getValues());
    }
    return q;
  }
}
