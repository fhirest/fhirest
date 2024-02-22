/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ee.fhir.fhirest.search.index;

import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

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
