package com.kodality.kefhir.scheduler.api;

import com.kodality.kefhir.scheduler.JobRepository;
import java.util.Date;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Singleton
public class SchedulerService {
  private final JobRepository jobRepository;

  public void schedule(String type, String identifier, Date scheduled) {
    jobRepository.insert(type, identifier, scheduled);
  }

  public void reschedule(String type, String identifier, Date scheduled) {
    jobRepository.cancel(type, identifier);
    jobRepository.insert(type, identifier, scheduled);
  }

  public void unschedule(String type, String identifier) {
    jobRepository.cancel(type, identifier);
  }

}
