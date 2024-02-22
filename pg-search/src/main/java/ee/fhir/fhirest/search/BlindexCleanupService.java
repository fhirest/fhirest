package ee.fhir.fhirest.search;

import ee.fhir.fhirest.search.repository.BlindexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
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
