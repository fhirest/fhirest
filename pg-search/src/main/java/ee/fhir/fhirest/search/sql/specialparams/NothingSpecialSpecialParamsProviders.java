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

package ee.fhir.fhirest.search.sql.specialparams;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.search.sql.ExpressionProvider;
import ee.fhir.fhirest.search.sql.params.DateExpressionProvider;
import ee.fhir.fhirest.util.sql.SqlBuilder;

public class NothingSpecialSpecialParamsProviders {

  public static class IdExpressionProvider extends ExpressionProvider {

    @Override
    public SqlBuilder makeExpression(QueryParam param, String alias) {
      return new SqlBuilder().in(alias + ".resource_id", param.getValues());
    }

    @Override
    public SqlBuilder order(String resourceType, String key, String alias, String direction) {
      return new SqlBuilder(alias + ".resource_id");
    }
  }

  public static class LastUpdatedExpressionProvider extends ExpressionProvider {

    @Override
    public SqlBuilder makeExpression(QueryParam param, String alias) {
      return DateExpressionProvider.makeExpression("tstzrange(" + alias + ".last_updated, " + alias + ".last_updated, '[]')" , param);
    }

    @Override
    public SqlBuilder order(String resourceType, String key, String alias, String direction) {
      return new SqlBuilder(alias + ".last_updated");
    }
  }

  public static class NotImlementedExpressionProvider extends ExpressionProvider {

    @Override
    public SqlBuilder makeExpression(QueryParam param, String alias) {
      throw new FhirException(FhirestIssue.FEST_001, param.getKey() + " search param");
    }

    @Override
    public SqlBuilder order(String resourceType, String key, String alias, String direction) {
      throw new FhirException(FhirestIssue.FEST_001, key + " search param");
    }
  }
}
