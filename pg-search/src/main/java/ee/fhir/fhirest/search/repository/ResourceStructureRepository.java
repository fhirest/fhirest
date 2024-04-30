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

package ee.fhir.fhirest.search.repository;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
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
