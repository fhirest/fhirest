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

  @Bean
  @ConditionalOnProperty("spring.datasource.store-app.url")
  @ConfigurationProperties("spring.datasource.store-app")
  public DataSourceProperties storeAppDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource storeAppDataSource(@Named("storeAppDataSourceProperties") Optional<DataSourceProperties> properties,
                                       @Named("defaultDataSource") Optional<DataSource> defaultDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> defaultDs).orElseThrow();
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.store-admin.url")
  @ConfigurationProperties("spring.datasource.store-admin")
  public DataSourceProperties storeAdminDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource storeAdminDataSource(@Named("storeAdminDataSourceProperties") Optional<DataSourceProperties> properties,
                                         @Named("adminDataSource") Optional<DataSource> adminDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> adminDs).orElseThrow();
  }


  @Bean
  public JdbcTemplate storeAppJdbcTemplate(@Named("storeAppDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  public JdbcTemplate storeAdminJdbcTemplate(@Named("storeAdminDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.store-app.url")
  public PgTransactionManager storeTransactionManager(@Named("storeAppDataSource") DataSource ds) {
    return new PgTransactionManager(ds);
  }
}
