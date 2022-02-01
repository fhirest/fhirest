package com.kodality.kefhir.search;

import com.kodality.kefhir.search.repository.BlindexRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class BlindexCleanupService {
  private final BlindexRepository blindexRepository;

  @Scheduled(cron = "0 0 * * * *")
  public void execute() {
    log.info("starting index cleanup");
    blindexRepository.cleanup();
    log.info("index cleanup finished");
  }

}
