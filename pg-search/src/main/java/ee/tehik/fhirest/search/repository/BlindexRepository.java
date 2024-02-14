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
package ee.tehik.fhirest.search.repository;

import ee.tehik.fhirest.PostgresListener.PostgresChangeListener;
import ee.tehik.fhirest.core.exception.FhirServerException;
import ee.tehik.fhirest.search.model.Blindex;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@Component
public class BlindexRepository {
  private static final Map<String, Map<String, Blindex>> INDEXES = new HashMap<>();
  @Inject
  @Named("searchAppJdbcTemplate")
  private JdbcTemplate jdbcTemplate;
  @Inject
  @Named("searchAdminJdbcTemplate")
  private JdbcTemplate adminJdbcTemplate;

  @PostConstruct
  @PostgresChangeListener(table = "search.blindex")
  public void refreshCache() {
    loadIndexes().forEach(p -> INDEXES.computeIfAbsent(p.getResourceType(), x -> new HashMap<>()).put(p.getPath(), p));
  }

  public static List<Blindex> getIndexes(String resourceType) {
    return INDEXES.containsKey(resourceType) ? new ArrayList<>(INDEXES.get(resourceType).values()) : List.of();
  }

  public static String getIndex(String resourceType, String path) {
    if (!INDEXES.containsKey(resourceType) || !INDEXES.get(resourceType).containsKey(path)) {
      throw new FhirServerException(500, resourceType + "." + path + " not indexed");
    }
    return INDEXES.get(resourceType).get(path).getName();
  }

  public List<Blindex> loadIndexes() {
    String sql = "SELECT * FROM search.blindex";
    return jdbcTemplate.query(sql, new BlindexRowMapper());
  }

  public Blindex createIndex(String paramType, String resourceType, String path) {
    return adminJdbcTemplate.queryForObject("SELECT * from search.create_blindex(?,?,?)", new BlindexRowMapper(), paramType, resourceType, path);
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
