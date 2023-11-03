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

import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;;
import jakarta.inject.Singleton;
import org.springframework.jdbc.core.JdbcTemplate;

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

  public void refresh() {
    RESOURCE_TYPES = adminJdbcTemplate.query("select * from search.resource_type", rs -> {
      Map<String, Long> map = new HashMap<>();
      while (rs.next()) {
        map.put(rs.getString("type"), rs.getLong("id"));
      }
      return map;
    });
  }

  public void defineResource(String type) {
    if (!RESOURCE_TYPES.containsKey(type)) {
      adminJdbcTemplate.queryForObject("select search.define_resource(?)", String.class, type);
    }
  }

}
