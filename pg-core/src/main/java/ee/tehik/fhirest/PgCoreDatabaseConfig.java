package ee.tehik.fhirest;

import jakarta.inject.Named;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class PgCoreDatabaseConfig {

  @Bean("defaultDsCfg")
  @ConditionalOnProperty("spring.datasource.default.url")
  @ConfigurationProperties("spring.datasource.default")
  public DataSourceProperties defaultDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean("defaultDs")
  @ConditionalOnBean(value = DataSourceProperties.class, name = "defaultDsCfg")
  public DataSource defaultDataSource(@Named("defaultDsCfg") DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().build();
  }


  @Bean("adminDsCfg")
  @ConditionalOnProperty("spring.datasource.admin.url")
  @ConfigurationProperties("spring.datasource.admin")
  public DataSourceProperties adminDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean("adminDs")
  public DataSource adminDataSource(@Named("adminDsCfg") Optional<DataSourceProperties> properties,
                                    @Named("defaultDs") Optional<DataSource> defaultDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> defaultDs).orElse(null);
  }

  @Bean
  @ConditionalOnBean(value = DataSourceProperties.class, name = "defaultDsCfg")
  public PgTransactionManager transactionManager(@Named("defaultDs") DataSource dataSource) {
    return new PgTransactionManager(dataSource);
  }

}
