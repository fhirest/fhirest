package ee.fhir.fhirest.search;

import ee.fhir.fhirest.PgTransactionManager;
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
public class PgSearchDatabaseConfig {

  @Bean
  @ConditionalOnProperty("spring.datasource.search-app.url")
  @ConfigurationProperties("spring.datasource.search-app")
  public DataSourceProperties searchAppDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource searchAppDataSource(@Named("searchAppDataSourceProperties") Optional<DataSourceProperties> properties,
                                        @Named("defaultDataSource") Optional<DataSource> defaultDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> defaultDs).orElseThrow();
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.search-admin.url")
  @ConfigurationProperties("spring.datasource.search-admin")
  public DataSourceProperties searchAdminDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource searchAdminDataSource(@Named("searchAdminDataSourceProperties") Optional<DataSourceProperties> properties,
                                          @Named("adminDataSource") Optional<DataSource> adminDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> adminDs).orElseThrow();
  }

  @Bean
  public JdbcTemplate searchAppJdbcTemplate(@Named("searchAppDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  public JdbcTemplate searchAdminJdbcTemplate(@Named("searchAdminDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.search-app.url")
  public PgTransactionManager searchTransactionManager(@Named("searchAppDataSource") DataSource ds) {
    return new PgTransactionManager(ds);
  }
}
