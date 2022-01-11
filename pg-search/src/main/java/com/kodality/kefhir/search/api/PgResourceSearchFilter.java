package com.kodality.kefhir.search.api;

import com.kodality.kefhir.util.sql.SqlBuilder;

public interface PgResourceSearchFilter {

  SqlBuilder filter(SqlBuilder builder, String alias);
}
