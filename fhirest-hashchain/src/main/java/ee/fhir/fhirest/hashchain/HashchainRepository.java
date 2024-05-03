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
