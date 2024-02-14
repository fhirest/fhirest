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


  @Bean("schedulerAppDsCfg")
  @ConditionalOnProperty("spring.datasource.scheduler-app.url")
  @ConfigurationProperties("spring.datasource.scheduler-app")
  public DataSourceProperties schedulerAppDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean("schedulerAppDs")
  public DataSource scheduerAppDataSource(@Named("schedulerAppDsCfg") Optional<DataSourceProperties> properties,
                                          @Named("defaultDs") Optional<DataSource> defaultDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> defaultDs).orElseThrow();
  }


  @Bean
  @Named("schedulerAppJdbcTemplate")
  public JdbcTemplate schedulerAppJdbcTemplate(@Named("schedulerAppDs") DataSource ds) {
    return new JdbcTemplate(ds);
  }

}
