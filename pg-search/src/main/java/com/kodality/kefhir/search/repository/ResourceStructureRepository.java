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
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.springframework.jdbc.core.JdbcTemplate;

import static java.util.stream.Collectors.joining;

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

  public void save(List<StructureElement> elements) {
    if (elements == null || elements.isEmpty()) {
      return;
    }

    // group by is just to avoid too many parameters
    Map<String, List<StructureElement>> groups = elements.stream().collect(Collectors.groupingBy(e -> e.getParent()));

    SqlBuilder dsb = new SqlBuilder("delete from search.resource_structure rs where not ").in("rs.parent", groups.keySet());
    adminJdbcTemplate.update(dsb.getSql(), dsb.getParams());

    groups.forEach((parent, el) -> {
      SqlBuilder sb = new SqlBuilder();
      sb.append("with t(child, alias, element_type) as (values ");
      sb.append(el.stream().map(e -> "(?,?,?)").collect(joining(",")));
      el.forEach(e -> sb.add(e.getChild(), e.getAlias(), e.getType()));
      sb.append(")");
      sb.append(", deleted as (delete from search.resource_structure rs where rs.parent = ? and (rs.child, rs.alias, rs.element_type) not in (select * from t))", parent);
      sb.append(", inserted as (insert into search.resource_structure (parent, child, alias, element_type) "
          + " select ?, t.* from t where (t.child, t.alias, t.element_type)"
          + " not in (select child, alias, element_type from search.resource_structure where parent = ?))", parent, parent);
      sb.append(" select 1");
      adminJdbcTemplate.queryForObject(sb.getSql(), Long.class, sb.getParams());
    });
  }


  public void refresh() {
    RESOURCE_TYPES = adminJdbcTemplate.query("select * from search.resource_type", rs -> {
      Map<String, Long> map = new HashMap<>();
      while (rs.next()) {
        map.put(rs.getString("type"), rs.getLong("id"));
      }
      return map;
    });
    adminJdbcTemplate.update("refresh materialized view search.resource_structure_recursive");
  }

  public void defineResource(String type) {
    adminJdbcTemplate.queryForObject("select search.define_resource(?)", String.class, type);
  }

}
