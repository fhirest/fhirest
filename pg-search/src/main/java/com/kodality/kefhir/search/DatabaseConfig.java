package com.kodality.kefhir.search;

import com.kodality.kefhir.PgTransactionManager;
import com.kodality.kefhir.core.util.BeanContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import java.util.Optional;
import javax.inject.Named;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

@Factory
public class DatabaseConfig {
    public DatabaseConfig(BeanContext bc) {
      // bc required to run migrations
    }

  @Bean
  @Requires(missingProperty = "datasources.search-app")
  @Named("search-app")
  public DataSource searchAppDataSource(@Named("default") DataSource defaultDs) {
    return defaultDs;
  }

  @Bean
  @Requires(missingProperty = "datasources.search-admin")
  @Named("search-admin")
  public DataSource storeAdminDataSource(@Named("default") Optional<DataSource> defaultDs, @Named("admin") Optional<DataSource> adminDs) {
    return adminDs.orElseGet(() -> defaultDs.orElseThrow());
  }

  @Bean
  @Named("searchAppJdbcTemplate")
  public JdbcTemplate searchAppJdbcTemplate(@Named("search-app") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Requires(property = "datasources.search-app")
  @Bean
  public PgTransactionManager transactionManager(@Named("search-app") DataSource ds) {
    return new PgTransactionManager(ds);
  }

  @Bean
  @Named("searchAdminJdbcTemplate")
  public JdbcTemplate searchAdminJdbcTemplate(@Named("search-admin") DataSource ds) {
    return new JdbcTemplate(ds);
  }

}
