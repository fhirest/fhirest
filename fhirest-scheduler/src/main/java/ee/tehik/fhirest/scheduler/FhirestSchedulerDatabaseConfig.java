package ee.tehik.fhirest.scheduler;

import jakarta.inject.Named;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

;

@Configuration
public class FhirestSchedulerDatabaseConfig {

  @Bean
  @ConditionalOnProperty("spring.datasource.scheduler-app.url")
  @ConfigurationProperties("spring.datasource.scheduler-app")
  public DataSourceProperties schedulerAppDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource schedulerAppDataSource(@Named("schedulerAppDataSourceProperties") Optional<DataSourceProperties> properties,
                                          @Named("defaultDataSource") Optional<DataSource> defaultDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> defaultDs).orElseThrow();
  }


  @Bean
  public JdbcTemplate schedulerAppJdbcTemplate(@Named("schedulerAppDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

}
