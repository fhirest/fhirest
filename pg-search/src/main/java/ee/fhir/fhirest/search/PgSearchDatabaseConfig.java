/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ee.fhir.fhirest.search;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ee.fhir.fhirest.PgTransactionManager;
import jakarta.inject.Named;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration
public class PgSearchDatabaseConfig {

  @Bean
  @ConditionalOnProperty("spring.datasource.search-app.jdbc-url")
  @ConfigurationProperties("spring.datasource.search-app")
  public HikariConfig searchAppDataSourceProperties() {
    return new HikariConfig();
  }

  @Bean
  public DataSource searchAppDataSource(@Named("searchAppDataSourceProperties") Optional<HikariConfig> properties,
                                        @Named("defaultDataSource") Optional<DataSource> defaultDs) {
    return properties.map(HikariDataSource::new)
        .or(() -> defaultDs.map(HikariDataSource.class::cast))
        .orElseThrow();
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.search-admin.jdbc-url")
  @ConfigurationProperties("spring.datasource.search-admin")
  public HikariConfig searchAdminDataSourceProperties() {
    return new HikariConfig();
  }

  @Bean
  public DataSource searchAdminDataSource(@Named("searchAdminDataSourceProperties") Optional<HikariConfig> properties,
                                          @Named("adminDataSource") Optional<DataSource> adminDs) {
    return properties.map(HikariDataSource::new)
        .or(() -> adminDs.map(HikariDataSource.class::cast))
        .orElseThrow();
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
  @ConditionalOnProperty("spring.datasource.search-app.jdbc-url")
  public PgTransactionManager searchTransactionManager(@Named("searchAppDataSource") DataSource ds) {
    return new PgTransactionManager(ds);
  }
}
