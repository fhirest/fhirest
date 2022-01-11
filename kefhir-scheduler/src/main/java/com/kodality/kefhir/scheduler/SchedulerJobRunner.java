package com.kodality.kefhir.scheduler;

import com.kodality.kefhir.scheduler.api.ScheduleJobRunner;
import io.micronaut.scheduling.annotation.Scheduled;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class SchedulerJobRunner {
  private final JobRepository jobRepository;
  private final List<ScheduleJobRunner> scheduleJobRunners;

  @Scheduled(cron = "0 0/5 * * * *")
  public void execute() {
    log.debug("starting scheduler job runner");
    List<SchedulerJob> jobs = jobRepository.getExecutables();
    if (jobs.isEmpty()) {
      log.debug("found 0 jobs");
      return;
    }
    log.info("found " + jobs.size() + " jobs");
    jobs.stream().forEach(job -> {
      if (!jobRepository.lock(job.getId())) {
        log.info("could not lock " + job.getId() + ", continuing");
        return;
      }
      try {
        scheduleJobRunners.stream()
            .filter(r -> r.getType().equals(job.getType()))
            .findFirst()
            .ifPresent(runner -> {
              String log = runner.run(job.getIdentifier());
              jobRepository.finish(job.getId(), log);
            });
        //TODO: think what to do if no runners found. they might be on other node!
      } catch (Throwable e) {
        jobRepository.fail(job.getId(), ExceptionUtils.getStackTrace(e));
        log.error("error during job " + job.getId() + "execution: ", e);
      }
    });
  }

}
