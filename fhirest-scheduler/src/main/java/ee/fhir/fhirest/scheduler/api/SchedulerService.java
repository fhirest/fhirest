package ee.fhir.fhirest.scheduler.api;

import ee.fhir.fhirest.scheduler.JobRepository;
import java.util.Date;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
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
