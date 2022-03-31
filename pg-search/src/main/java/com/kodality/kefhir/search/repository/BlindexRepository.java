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
package com.kodality.kefhir.search.repository;

import com.kodality.kefhir.PostgresListener.PostgresChangeListener;
import com.kodality.kefhir.core.exception.FhirServerException;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.search.model.Blindex;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@Singleton
public class BlindexRepository {
  private static final Map<String, String> INDEXES = new HashMap<>();
  @Inject
  @Named("searchAppJdbcTemplate")
  private JdbcTemplate jdbcTemplate;
  @Inject
  @Named("searchAdminJdbcTemplate")
  private JdbcTemplate adminJdbcTemplate;

  @PostConstruct
  @PostgresChangeListener(table="search.blindex")
  public void refreshCache() {
    loadIndexes().forEach(p -> INDEXES.put(p.getResourceType() + "." + p.getPath(), p.getName()));
  }

  public static String getIndex(String resourceType, String path) {
    String key = resourceType + "." + path;
    if (!INDEXES.containsKey(key)) {
      throw new FhirServerException(500, key + " not indexed");
    }
    return INDEXES.get(key);
  }

  public List<Blindex> loadIndexes() {
    String sql = "SELECT * FROM search.blindex";
    return jdbcTemplate.query(sql, new BlindexRowMapper());
  }

  public Blindex createIndex(String paramType, String resourceType, String path) {
    return adminJdbcTemplate.queryForObject("SELECT * from search.create_blindex(?,?,?)", new BlindexRowMapper(), paramType, resourceType, path);
  }

  public void merge(Long blindexId, ResourceId id, String jsonContent) {
    String sql = "select search.merge_blindex(?, sid, ?::jsonb) from search.resource where resource_type = search.resource_type_id(?) and resource_id = ?";
    adminJdbcTemplate.queryForObject(sql, Object.class, blindexId, jsonContent, id.getResourceType(), id.getResourceId());
  }

  public void dropIndex(String paramType, String resourceType, String path) {
    adminJdbcTemplate.queryForObject("SELECT search.drop_blindex(?,?,?)", String.class, paramType, resourceType, path);
  }

  public void cleanup() {
    adminJdbcTemplate.queryForObject("SELECT search.cleanup_indexes()", Object.class);
  }

  private static class BlindexRowMapper implements RowMapper<Blindex> {

    @Override
    public Blindex mapRow(ResultSet rs, int rowNum) throws SQLException {
      Blindex p = new Blindex();
      p.setId(rs.getLong("id"));
      p.setResourceType(rs.getString("resource_type"));
      p.setPath(rs.getString("path"));
      p.setName(rs.getString("index_name"));
      p.setParamType(rs.getString("param_type"));
      return p;
    }

  }

}
