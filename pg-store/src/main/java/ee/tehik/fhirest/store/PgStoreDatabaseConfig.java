package ee.tehik.fhirest.store;

import ee.tehik.fhirest.PgTransactionManager;
import jakarta.inject.Named;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration
public class PgStoreDatabaseConfig {

  @Bean("storeAppDsCfg")
  @ConditionalOnProperty("spring.datasource.store-app.url")
  @ConfigurationProperties("spring.datasource.store-app")
  public DataSourceProperties storeAppDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean("storeAppDs")
  public DataSource storeAppDataSource(@Named("storeAppDsCfg") Optional<DataSourceProperties> properties,
                                       @Named("defaultDs") Optional<DataSource> defaultDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> defaultDs).orElseThrow();
  }

  @Bean("storeAdminDsCfg")
  @ConditionalOnProperty("spring.datasource.store-admin.url")
  @ConfigurationProperties("spring.datasource.store-admin")
  public DataSourceProperties storeAdminDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean("storeAdminDs")
  public DataSource storeAdminDataSource(@Named("storeAdminDsCfg") Optional<DataSourceProperties> properties,
                                         @Named("adminDs") Optional<DataSource> adminDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> adminDs).orElseThrow();
  }


  @Bean
  @Named("storeAppJdbcTemplate")
  public JdbcTemplate storeAppJdbcTemplate(@Named("storeAppDs") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  @Named("storeAdminJdbcTemplate")
  public JdbcTemplate storeAdminJdbcTemplate(@Named("storeAdminDs") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.store-app.url")
  public PgTransactionManager transactionManager(@Named("storeAppDs") DataSource ds) {
    return new PgTransactionManager(ds);
  }
}
