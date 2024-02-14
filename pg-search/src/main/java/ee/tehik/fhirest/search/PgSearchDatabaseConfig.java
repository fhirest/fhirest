package ee.tehik.fhirest.search;

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
public class PgSearchDatabaseConfig {

  @Bean("searchAppDsCfg")
  @ConditionalOnProperty("spring.datasource.search-app.url")
  @ConfigurationProperties("spring.datasource.search-app")
  public DataSourceProperties searchAppDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean("searchAppDs")
  public DataSource searchAppDataSource(@Named("searchAppDsCfg") Optional<DataSourceProperties> properties,
                                        @Named("defaultDs") Optional<DataSource> defaultDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> defaultDs).orElseThrow();
  }

  @Bean("searchAdminDsCfg")
  @ConditionalOnProperty("spring.datasource.search-admin.url")
  @ConfigurationProperties("spring.datasource.search-admin")
  public DataSourceProperties searchAdminDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean("searchAdminDs")
  public DataSource searchAdminDataSource(@Named("searchAdminDsCfg") Optional<DataSourceProperties> properties,
                                          @Named("adminDs") Optional<DataSource> adminDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> adminDs).orElseThrow();
  }

  @Bean("searchAppJdbcTemplate")
  public JdbcTemplate searchAppJdbcTemplate(@Named("searchAppDs") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean("searchAdminJdbcTemplate")
  public JdbcTemplate searchAdminJdbcTemplate(@Named("searchAdminDs") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @ConditionalOnProperty("spring.datasource.search-app.url")
  @Bean
  public PgTransactionManager transactionManager(@Named("searchAppDs") DataSource ds) {
    return new PgTransactionManager(ds);
  }
}
