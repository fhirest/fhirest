/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
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

package ee.fhir.fhirest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.inject.Named;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class PgCoreDatabaseConfig {

  @Bean
  @ConditionalOnProperty("spring.datasource.hikari.jdbc-url")
  @ConfigurationProperties("spring.datasource.hikari")
  public HikariConfig springDataSourceProperties() {
    return new HikariConfig();
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.default.jdbc-url")
  @ConfigurationProperties("spring.datasource.default")
  public HikariConfig defaultDataSourceProperties() {
    return new HikariConfig();
  }

  @Bean
  @Conditional({OnDefaultDatasourceProperties.class})
  public DataSource defaultDataSource(@Named("defaultDataSourceProperties") Optional<HikariConfig> defaultProps,
                                      @Named("springDataSourceProperties") Optional<HikariConfig> springProps) {
    return defaultProps.map(HikariDataSource::new)
        .or(() -> springProps.map(HikariDataSource::new))
        .orElse(null);
  }

  @Bean
  @ConditionalOnProperty("spring.datasource.admin.jdbc-url")
  @ConfigurationProperties("spring.datasource.admin")
  public HikariConfig adminDataSourceProperties() {
    return new HikariConfig();
  }

  @Bean
  public DataSource adminDataSource(@Named("adminDataSourceProperties") Optional<HikariConfig> properties,
                                    @Named("defaultDataSource") Optional<DataSource> defaultDs) {
    return properties.map(HikariDataSource::new)
        .or(() -> defaultDs.map(HikariDataSource.class::cast))
        .orElse(null);
  }

  @Bean
  @ConditionalOnBean(value = DataSource.class, name = "defaultDataSource")
  public PgTransactionManager defaultTransactionManager(@Named("defaultDataSource") DataSource dataSource) {
    return new PgTransactionManager(dataSource);
  }

  private static class OnDefaultDatasourceProperties extends AnyNestedCondition {

    OnDefaultDatasourceProperties() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnBean(value = HikariConfig.class, name = "defaultDataSourceProperties")
    static class OnDefaultDsProps {}

    @ConditionalOnBean(value = HikariConfig.class, name = "springDataSourceProperties")
    static class OnSpringDsProps {}

  }

}
