/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
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

package ee.fhir.fhirest.search.repository;

import ee.fhir.fhirest.PostgresListener.PostgresChangeListener;
import ee.fhir.fhirest.core.exception.FhirServerException;
import ee.fhir.fhirest.search.model.Blindex;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

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
      throw new FhirServerException(resourceType + "." + path + " not indexed");
    }
    Blindex b = INDEXES.get(resourceType).get(path);
    String physical = computePhysicalName(b.getResourceType(), b.getParamType(), b.getPath());
    return "search." + physical;
  }

  private static String computePhysicalName(String resourceType, String paramType, String path) {
    String base = (resourceType + "_" + paramType + "_" +
        path.replaceAll("[^a-zA-Z0-9_]", "_").replace(".", "_")).toLowerCase();
    // cap at 63 chars (same as SQL)
    return base.length() > 63 ? base.substring(0, 63) : base;
  }

  public List<Blindex> loadIndexes() {
    String sql = "SELECT * FROM search.blindex";
    return jdbcTemplate.query(sql, new BlindexRowMapper());
  }

  public Blindex createIndex(String paramType, String resourceType, String path, String spCode) {
    return adminJdbcTemplate.queryForObject("SELECT * from search.create_blindex(?,?,?,?)",
      new BlindexRowMapper(), paramType, resourceType, path, spCode);
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
