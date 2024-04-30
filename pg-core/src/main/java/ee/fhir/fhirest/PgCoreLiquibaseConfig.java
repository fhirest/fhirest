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

package ee.fhir.fhirest;

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
