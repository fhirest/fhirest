package ee.fhir.fhirest.search.api;

import ee.fhir.fhirest.util.sql.SqlBuilder;

public interface PgResourceSearchFilter {

  SqlBuilder filter(SqlBuilder builder, String alias);
}
