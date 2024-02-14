package ee.tehik.fhirest.scheduler;

import ee.tehik.fhirest.scheduler.api.ScheduleJobRunner;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
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
      } catch (Throwable e) {
        jobRepository.fail(job.getId(), ExceptionUtils.getStackTrace(e));
        log.error("error during job " + job.getId() + "execution: ", e);
      }
    });
  }

}
