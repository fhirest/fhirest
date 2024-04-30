/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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

package ee.fhir.fhirest.scheduler;

import ee.fhir.fhirest.scheduler.api.ScheduleJobRunner;
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
