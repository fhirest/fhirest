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
package ee.fhir.fhirest.hashchain;

import ee.fhir.fhirest.core.model.VersionId;
import jakarta.inject.Named;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class HashchainRepository {
  private final JdbcTemplate jdbcTemplate;

  public HashchainRepository(@Named("storeAppJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Retryable(maxAttempts = 10, backoff = @Backoff(delay = 100L, random = true), retryFor = DuplicateKeyException.class)
  public void storeHashchain(VersionId id) {
    jdbcTemplate.queryForObject("SELECT store.store_hashchain(?::text, ?::text, ?::smallint)", Object.class,
        id.getResourceType(), id.getResourceId(), id.getVersion());
  }

  public boolean validateHash(VersionId id) {
    return jdbcTemplate.queryForObject("SELECT (calculated = stored) AS valid FROM store.validate_resource_hash(?::text, ?::smallint)", Boolean.class,
        id.getResourceType(), id.getResourceId(), id.getVersion());
  }
}
