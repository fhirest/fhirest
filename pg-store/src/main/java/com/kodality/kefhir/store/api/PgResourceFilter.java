package com.kodality.kefhir.store.api;

import com.kodality.kefhir.util.sql.SqlBuilder;

public interface PgResourceFilter {

  SqlBuilder filter(SqlBuilder builder, String alias);
}
