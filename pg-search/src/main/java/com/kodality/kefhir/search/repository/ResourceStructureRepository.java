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

import com.kodality.kefhir.search.model.StructureElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.springframework.jdbc.core.JdbcTemplate;

import static java.util.stream.Collectors.toList;

@Singleton
public class ResourceStructureRepository {
  private static Map<String, Long> RESOURCE_TYPES = new HashMap<>();
  @Inject
  @Named("searchAdminJdbcTemplate")
  private JdbcTemplate adminJdbcTemplate;

  public static Long getTypeId(String type) {
    if (!RESOURCE_TYPES.containsKey(type)) {
      throw new RuntimeException(type + " isn't defined or not yet loaded");
    }
    return RESOURCE_TYPES.get(type);
  }

  public void create(List<StructureElement> elements) {
    String sql = "INSERT INTO search.resource_structure (base, path, element_type, is_many) VALUES (?,?,?,?)";
    List<Object[]> args = elements.stream()
        .map(el -> new Object[]{el.getBase(), el.getPath(), el.getType(), el.isMany()})
        .collect(toList());
    adminJdbcTemplate.batchUpdate(sql, args);
  }

  public void refresh() {
    adminJdbcTemplate.update("refresh materialized view search.resource_structure_recursive");
    RESOURCE_TYPES = adminJdbcTemplate.query("select * from search.resource_type", rs -> {
      Map<String, Long> map = new HashMap<>();
      while(rs.next()) {
        map.put(rs.getString("type"), rs.getLong("id"));
      }
      return map;
    });
  }

  public void deleteAll() {
    adminJdbcTemplate.update("DELETE FROM search.resource_structure");
  }

  public void defineResource(String type) {
    adminJdbcTemplate.queryForObject("select search.define_resource(?)", String.class, type);
  }

}
