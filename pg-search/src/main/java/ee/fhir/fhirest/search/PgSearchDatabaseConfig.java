/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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

import ee.fhir.fhirest.PgTransactionManager;
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

  @Bean
  @ConditionalOnProperty("spring.datasource.search-app.url")
  @ConfigurationProperties("spring.datasource.search-app")
  public DataSourceProperties searchAppDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource searchAppDataSource(@Named("searchAppDataSourceProperties") Optional<DataSourceProperties> properties,
                                        @Named("defaultDataSource") Optional<DataSource> defaultDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> defaultDs).orElseThrow();
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.search-admin.url")
  @ConfigurationProperties("spring.datasource.search-admin")
  public DataSourceProperties searchAdminDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  public DataSource searchAdminDataSource(@Named("searchAdminDataSourceProperties") Optional<DataSourceProperties> properties,
                                          @Named("adminDataSource") Optional<DataSource> adminDs) {
    return properties.map(p -> (DataSource) p.initializeDataSourceBuilder().build()).or(() -> adminDs).orElseThrow();
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
  @ConditionalOnProperty("spring.datasource.search-app.url")
  public PgTransactionManager searchTransactionManager(@Named("searchAppDataSource") DataSource ds) {
    return new PgTransactionManager(ds);
  }
}
