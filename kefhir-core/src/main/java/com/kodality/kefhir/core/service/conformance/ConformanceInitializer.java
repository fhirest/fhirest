package com.kodality.kefhir.core.service.conformance;

import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class ConformanceInitializer {

  private final ConformanceInitializationService conformanceInitializationService;

  @EventListener
  @Async
  public void init(final StartupEvent event) {
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    conformanceInitializationService.refresh();
  }
}
