package com.kodality.kefhir;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

@Factory
public class DatabaseConfig {
  @Inject
  private DataSource dataSource;

  @Bean
  @Primary
  @Singleton
  public JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(dataSource);
  }

}
