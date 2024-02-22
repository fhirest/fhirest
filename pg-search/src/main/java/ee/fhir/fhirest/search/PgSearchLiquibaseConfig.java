package ee.fhir.fhirest.search;

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
public class PgSearchLiquibaseConfig {

  @Bean
  @ConditionalOnProperty("spring.datasource.search-app.liquibase.change-log")
  @ConfigurationProperties(prefix = "spring.datasource.search-app.liquibase")
  public LiquibaseProperties searchAppLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.search-admin.liquibase.change-log")
  @ConfigurationProperties(prefix = "spring.datasource.search-admin.liquibase")
  public LiquibaseProperties searchAdminLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @ConditionalOnBean(value = LiquibaseProperties.class, name = "searchAppLiquibaseProperties")
  public SpringLiquibase searchAppLiquibase(@Named("searchAppDataSource") DataSource dataSource,
                                          @Named("searchAppLiquibaseProperties") LiquibaseProperties properties) {
    return SpringLiquibaseBuilder.build(dataSource, properties);
  }

  @Bean
  @ConditionalOnBean(value = LiquibaseProperties.class, name = "searchAdminLiquibaseProperties")
  public SpringLiquibase searchAdminLiquibase(@Named("searchAdminDataSource") DataSource dataSource,
                                        @Named("searchAdminLiquibaseProperties") LiquibaseProperties properties) {
    return SpringLiquibaseBuilder.build(dataSource, properties);
  }

}
