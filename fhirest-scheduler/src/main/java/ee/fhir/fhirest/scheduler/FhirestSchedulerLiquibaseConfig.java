package ee.fhir.fhirest.scheduler;

import ee.fhir.fhirest.SpringLiquibaseBuilder;
import jakarta.inject.Named;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class FhirestSchedulerLiquibaseConfig {

  @Bean
  @ConditionalOnProperty("spring.datasource.scheduler-app.liquibase.change-log")
  @ConfigurationProperties(prefix = "spring.datasource.scheduler-app.liquibase")
  public LiquibaseProperties schedulerAppLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @ConditionalOnBean(value = LiquibaseProperties.class, name = "schedulerAppLiquibaseProperties")
  public SpringLiquibase schedulerLiquibase(@Named("schedulerAppDataSource") DataSource dataSource,
                                          @Named("schedulerAppLiquibaseProperties") LiquibaseProperties properties) {
    return SpringLiquibaseBuilder.build(dataSource, properties);
  }

}
