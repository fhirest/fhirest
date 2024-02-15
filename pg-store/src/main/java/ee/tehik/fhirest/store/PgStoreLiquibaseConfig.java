package ee.tehik.fhirest.store;

import ee.tehik.fhirest.SpringLiquibaseBuilder;
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
public class PgStoreLiquibaseConfig {

  @Bean
  @ConditionalOnProperty("spring.datasource.store-app.liquibase.change-log")
  @ConfigurationProperties(prefix = "spring.datasource.store-app.liquibase")
  public LiquibaseProperties storeAppLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.store-admin.liquibase.change-log")
  @ConfigurationProperties(prefix = "spring.datasource.store-admin.liquibase")
  public LiquibaseProperties storeAdminLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @ConditionalOnBean(value = LiquibaseProperties.class, name = "storeAppLiquibaseProperties")
  public SpringLiquibase storeAppLiquibase(@Named("storeAppDataSource") DataSource dataSource,
                                          @Named("storeAppLiquibaseProperties") LiquibaseProperties properties) {
    return SpringLiquibaseBuilder.build(dataSource, properties);
  }

  @Bean
  @ConditionalOnBean(value = LiquibaseProperties.class, name = "storeAdminLiquibaseProperties")
  public SpringLiquibase storeAdminLiquibase(@Named("storeAdminDataSource") DataSource dataSource,
                                        @Named("storeAdminLiquibaseProperties") LiquibaseProperties properties) {
    return SpringLiquibaseBuilder.build(dataSource, properties);
  }

}
