package ee.tehik.fhirest.store.api;

import ee.tehik.fhirest.util.sql.SqlBuilder;

public interface PgResourceFilter {

  SqlBuilder filter(SqlBuilder builder, String alias);
}
