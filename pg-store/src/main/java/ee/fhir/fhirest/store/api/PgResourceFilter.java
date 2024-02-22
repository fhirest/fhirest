package ee.fhir.fhirest.store.api;

import ee.fhir.fhirest.util.sql.SqlBuilder;

public interface PgResourceFilter {

  SqlBuilder filter(SqlBuilder builder, String alias);
}
