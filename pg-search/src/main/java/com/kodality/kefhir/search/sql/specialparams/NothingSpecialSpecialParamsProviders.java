package com.kodality.kefhir.search.sql.specialparams;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.search.QueryParam;
import com.kodality.kefhir.search.sql.ExpressionProvider;
import com.kodality.kefhir.search.sql.params.DateExpressionProvider;
import com.kodality.kefhir.util.sql.SqlBuilder;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

public class NothingSpecialSpecialParamsProviders {

  public static class IdExpressionProvider extends ExpressionProvider {

    @Override
    public SqlBuilder makeExpression(QueryParam param, String alias) {
      return new SqlBuilder().in(alias + ".resource_id", param.getValues());
    }

    @Override
    public SqlBuilder order(String resourceType, String key, String alias) {
      return new SqlBuilder(alias + ".resource_id");
    }
  }

  public static class LastUpdatedExpressionProvider extends ExpressionProvider {

    @Override
    public SqlBuilder makeExpression(QueryParam param, String alias) {
      return DateExpressionProvider.makeExpression(alias + ".last_updated", param);
    }

    @Override
    public SqlBuilder order(String resourceType, String key, String alias) {
      return new SqlBuilder(alias + ".last_updated");
    }
  }

  public static class NotImlementedExpressionProvider extends ExpressionProvider {

    @Override
    public SqlBuilder makeExpression(QueryParam param, String alias) {
      throw new FhirException(400, IssueType.NOTSUPPORTED, param.getKey() + " search param not implemented");
    }

    @Override
    public SqlBuilder order(String resourceType, String key, String alias) {
      throw new FhirException(400, IssueType.NOTSUPPORTED, key + " search param not implemented");
    }
  }
}
