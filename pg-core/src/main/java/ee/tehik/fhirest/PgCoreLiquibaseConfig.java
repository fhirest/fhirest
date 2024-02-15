package ee.tehik.fhirest;

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
public class PgCoreLiquibaseConfig {

  @Bean
  @ConditionalOnProperty("spring.datasource.default.liquibase.change-log")
  @ConfigurationProperties(prefix = "spring.datasource.default.liquibase")
  public LiquibaseProperties defaultLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.admin.liquibase.change-log")
  @ConfigurationProperties(prefix = "spring.datasource.admin.liquibase")
  public LiquibaseProperties adminLiquibaseProperties() {
    return new LiquibaseProperties();
  }

  @Bean
  @ConditionalOnBean(value = LiquibaseProperties.class, name = "defaultLiquibaseProperties")
  public SpringLiquibase defaultLiquibase(@Named("defaultDataSource") DataSource dataSource,
                                          @Named("defaultLiquibaseProperties") LiquibaseProperties properties) {
    return SpringLiquibaseBuilder.build(dataSource, properties);
  }

  @Bean
  @ConditionalOnBean(value = LiquibaseProperties.class, name = "adminLiquibaseProperties")
  public SpringLiquibase adminLiquibase(@Named("adminDataSource") DataSource dataSource, @Named("adminLiquibaseProperties") LiquibaseProperties properties) {
    return SpringLiquibaseBuilder.build(dataSource, properties);
  }

}
