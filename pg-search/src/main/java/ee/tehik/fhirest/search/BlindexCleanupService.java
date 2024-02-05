package ee.tehik.fhirest.search;

import ee.tehik.fhirest.search.repository.BlindexRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class BlindexCleanupService {
  private final BlindexRepository blindexRepository;

  @Scheduled(cron = "0 0 * * * *")
  public void execute() {
    log.debug("starting index cleanup");
    blindexRepository.cleanup();
    log.debug("index cleanup finished");
  }

}
