package ee.tehik.fhirest.search.api;

import ee.tehik.fhirest.util.sql.SqlBuilder;

public interface PgResourceSearchFilter {

  SqlBuilder filter(SqlBuilder builder, String alias);
}
