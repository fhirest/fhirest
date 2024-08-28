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

import ee.fhir.fhirest.util.sql.SqlBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchedulerJobRepository {
  @Inject
  @Named("schedulerAppJdbcTemplate")
  private JdbcTemplate jdbcTemplate;

  public void insert(String type, String identifier, Date scheduled) {
    String sql = "INSERT INTO scheduler.job (type, identifier, scheduled) VALUES (?,?,?)";
    jdbcTemplate.update(sql, type, identifier, scheduled);
  }

  public void cancel(String type, String identifier) {
//    String sql = "UPDATE scheduler.job SET status = 'cancelled' where type = ? and identifier = ? and status = 'active'";
    String sql = "DELETE FROM scheduler.job WHERE type = ? AND identifier = ? AND status = 'active'";
    jdbcTemplate.update(sql, type, identifier);
  }

  public boolean lock(Long id) {
    String sql = "UPDATE scheduler.job SET started = now() WHERE id = ? AND started IS NULL AND status = 'active'";
    return jdbcTemplate.update(sql, id) > 0;
  }

  public void finish(Long id, String log) {
    String sql = "UPDATE scheduler.job SET finished = now(), status = 'finished', log = ? WHERE id = ?";
    jdbcTemplate.update(sql, log, id);
  }

  public void fail(Long id, String log) {
    String sql = "UPDATE scheduler.job SET finished = now(), status = 'failed', log = ? WHERE id = ?";
    jdbcTemplate.update(sql, log, id);
  }

  public void markRerun(Long id) {
    String sql = "UPDATE scheduler.job SET status = 'rerun' WHERE id = ? and status = 'failed'";
    jdbcTemplate.update(sql, id);
  }

  public List<SchedulerJob> getExecutables() {
    String sql = "SELECT * FROM scheduler.job WHERE started IS NULL AND status = 'active' AND scheduled <= now()";
    return jdbcTemplate.query(sql, this::rowMapper);
  }

  public List<SchedulerJob> query(SchedulerJobQueryParams params) {
    SqlBuilder sb = new SqlBuilder("SELECT * FROM scheduler.job j where 1=1");
    sb.appendIfNotNull(" and j.id = ?", params.getId());
    sb.appendIfNotNull(" and j.status = ?", params.getStatus());
    return jdbcTemplate.query(sb.getSql(), this::rowMapper, sb.getParams());
  }

  private SchedulerJob rowMapper(ResultSet rs, int index) throws SQLException {
    return new SchedulerJob()
        .setId(rs.getLong("id"))
        .setType(rs.getString("type"))
        .setIdentifier(rs.getString("identifier"))
        .setScheduled(rs.getObject("scheduled", OffsetDateTime.class))
        .setStarted(rs.getObject("started", LocalDateTime.class))
        .setStarted(rs.getObject("finished", LocalDateTime.class))
        .setLog(rs.getString("log"))
        .setStatus(rs.getString("status"));
  }

}
