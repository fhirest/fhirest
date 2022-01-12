package com.kodality.kefhir;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import javax.inject.Named;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

@Factory
public class DatabaseConfig {

  @Bean
  @Primary
  public JdbcTemplate jdbcTemplate(@Primary DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  @Named("adminJdbcTemplate")
  public JdbcTemplate adminJdbcTemplate(@Named("admin") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

}
