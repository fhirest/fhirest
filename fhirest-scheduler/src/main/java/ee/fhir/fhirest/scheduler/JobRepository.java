/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobRepository {
  @Inject
  @Named("schedulerAppJdbcTemplate")
  private JdbcTemplate jdbcTemplate;

  public void insert(String type, String identifier, Date scheduled) {
    String sql = "INSERT INTO scheduler.job (type, identifier, scheduled) values (?,?,?)";
    jdbcTemplate.update(sql, type, identifier, scheduled);
  }

  public void cancel(String type, String identifier) {
//    String sql = "UPDATE scheduler.job SET status = 'cancelled' where type = ? and identifier = ? and status = 'active'";
    String sql = "DELETE FROM scheduler.job where type = ? and identifier = ? and status = 'active'";
    jdbcTemplate.update(sql, type, identifier);
  }

  public boolean lock(Long id) {
    String sql = "UPDATE scheduler.job SET started = now() where id = ? and started is null and status = 'active'";
    return jdbcTemplate.update(sql, id) > 0;
  }

  public void finish(Long id, String log) {
    String sql = "UPDATE scheduler.job SET finished = now(), status = 'finished', log = ? where id = ?";
    jdbcTemplate.update(sql, log, id);
  }

  public void fail(Long id, String log) {
    String sql = "UPDATE scheduler.job SET finished = now(), status = 'failed', log = ? where id = ?";
    jdbcTemplate.update(sql, log, id);
  }

  public List<SchedulerJob> getExecutables() {
    String sql =
        "SELECT id, type, identifier FROM scheduler.job WHERE started is null and status = 'active' and scheduled <= now()";
    return jdbcTemplate.query(sql, (rs, i) -> {
      return new SchedulerJob(rs.getLong(1), rs.getString(2), rs.getString(3));
    });
  }

}
