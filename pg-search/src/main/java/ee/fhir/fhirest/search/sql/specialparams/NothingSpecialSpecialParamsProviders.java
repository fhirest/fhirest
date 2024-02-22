package ee.fhir.fhirest.search.sql.specialparams;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.search.sql.ExpressionProvider;
import ee.fhir.fhirest.search.sql.params.DateExpressionProvider;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

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
      throw new FhirException(400, IssueType.NOTSUPPORTED, param.getKey() + " search param not implemented");
    }

    @Override
    public SqlBuilder order(String resourceType, String key, String alias, String direction) {
      throw new FhirException(400, IssueType.NOTSUPPORTED, key + " search param not implemented");
    }
  }
}
