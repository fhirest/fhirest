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

package ee.fhir.fhirest.search.index;

import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchIndexRepository {
  @Inject
  @Named("searchAppJdbcTemplate")
  private JdbcTemplate jdbcTemplate;

  public Long saveResource(ResourceVersion version) {
//    if (version.getId().getVersion() == 1) {
//      String sql = "insert into search.resource(resource_type, resource_id, last_updated, active) select search.resource_type_id(?), ?, ?, true returning sid";
//      return jdbcTemplate.queryForObject(sql, Long.class, version.getId().getResourceType(), version.getId().getResourceId(), version.getModified());
//    }
    SqlBuilder sb = new SqlBuilder();
    sb.append("WITH upd AS (");
    sb.append(" update search.resource r set last_updated = ?, active = true where resource_type = search.rt_id(?) and resource_id = ?",
        version.getModified(), version.getId().getResourceType(), version.getId().getResourceId());
    sb.append(" RETURNING sid)");
    sb.append(" , ins as (insert into search.resource(resource_type, resource_id, last_updated, active) select search.rt_id(?), ?, ?, true",
        version.getId().getResourceType(), version.getId().getResourceId(), version.getModified());
    sb.append(" WHERE NOT EXISTS (SELECT * FROM upd) returning sid)");
    sb.append(" select coalesce((select sid from upd), (select sid from ins))");
    return jdbcTemplate.queryForObject(sb.getSql(), Long.class, sb.getParams());
  }

  public Long deleteResource(ResourceId resourceId) {
    try {
      String sql =
          "update search.resource r set active = false where resource_type = (select search.rt_id(?)) and resource_id = ? and active = true returning sid";
      return jdbcTemplate.queryForObject(sql, Long.class, resourceId.getResourceType(), resourceId.getResourceId());
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public Long getResourceSid(ResourceId resourceId) {
    String sql = "select sid from search.resource where resource_type = search.rt_id(?) and resource_id = ? and active = true";
    return jdbcTemplate.queryForObject(sql, Long.class, resourceId.getResourceType(), resourceId.getResourceId());
  }

}
