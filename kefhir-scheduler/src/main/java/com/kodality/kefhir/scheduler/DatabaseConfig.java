package com.kodality.kefhir.scheduler;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import javax.inject.Named;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

@Factory
public class DatabaseConfig {

  @Bean
  @Requires(missingProperty = "datasources.scheduler-app")
  @Named("scheduler-app")
  public DataSource searchAppDataSource(@Named("default") DataSource defaultDs) {
    return defaultDs;
  }

  @Bean
  @Named("schedulerAppJdbcTemplate")
  public JdbcTemplate searchAppJdbcTemplate(@Named("scheduler-app") DataSource ds) {
    return new JdbcTemplate(ds);
  }

}
