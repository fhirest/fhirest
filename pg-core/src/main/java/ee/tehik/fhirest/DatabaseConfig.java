package ee.tehik.fhirest;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Named;;
import javax.sql.DataSource;

@Factory
public class DatabaseConfig {

  @Bean
  @Requires(property = "datasources.default")
  public PgTransactionManager transactionManager(@Named("default") DataSource dataSource) {
    return new PgTransactionManager(dataSource);
  }

}
