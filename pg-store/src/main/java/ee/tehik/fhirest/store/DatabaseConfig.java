package ee.tehik.fhirest.store;

import ee.tehik.fhirest.PgTransactionManager;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import java.util.Optional;
import jakarta.inject.Named;;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

@Factory
public class DatabaseConfig {

  @Bean
  @Requires(missingProperty = "datasources.store-app")
  @Named("store-app")
  public DataSource storeAppDataSource(@Named("default") DataSource defaultDs) {
    return defaultDs;
  }

  @Bean
  @Requires(missingProperty = "datasources.store-admin")
  @Named("store-admin")
  public DataSource storeAdminDataSource(@Named("default") Optional<DataSource> defaultDs, @Named("admin") Optional<DataSource> adminDs) {
    return adminDs.orElseGet(() -> defaultDs.orElseThrow());
  }

  @Bean
  @Named("storeAppJdbcTemplate")
  public JdbcTemplate storeAppJdbcTemplate(@Named("store-app") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  @Requires(property = "datasources.store-app")
  public PgTransactionManager transactionManager(@Named("store-app") DataSource ds) {
    return new PgTransactionManager(ds);
  }

  @Bean
  @Named("storeAdminJdbcTemplate")
  public JdbcTemplate storeAdminJdbcTemplate(@Named("store-admin") DataSource ds) {
    return new JdbcTemplate(ds);
  }

}
