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

import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.search.model.Blindex;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class TypeIndexRepository<T> {
  @Inject
  @Named("searchAppJdbcTemplate")
  protected JdbcTemplate jdbcTemplate;

  public abstract String getType();

  protected abstract String fields();

  protected abstract SqlBuilder withValues(List<T> values);

  public abstract Stream<T> map(Object value, String valueType);

  public void save(Long sid, ResourceVersion version, Blindex blindex, List<T> values) {
    if (version != null && version.getId().getVersion() == 1) {
      create(sid, blindex, values);
    } else {
      update(sid, blindex, values);
    }
  }

  protected void update(Long sid, Blindex blindex, List<T> values) {
    String name = "search." + blindex.getName();
    String fields = fields();
    if (CollectionUtils.isEmpty(values)) {
      jdbcTemplate.update("update " + name + " set active = false where sid = ? and active = true", sid);
      return;
    }
    SqlBuilder sb = new SqlBuilder();
    sb.append("with vals(" + fields + ") as (values");
    sb.append(withValues(values));
    sb.append(")");

    sb.append(", deleted as (update " + name + " set active = false where sid = ? and active = true and (" + fields + ") not in (select * from vals))", sid);
    sb.append(", created as (insert into " + name + "(sid, blindex_id, " + fields + ")" +
            " select ?, ?, vals.* from vals where (" + fields + ") not in (select " + fields + " from " + name + " where active = true and sid = ?))", sid,
        blindex.getId(), sid);
    sb.append("select 1");
    jdbcTemplate.queryForObject(sb.getSql(), Object.class, sb.getParams());
  }

  protected void create(Long sid, Blindex blindex, List<T> values) {
    if (CollectionUtils.isEmpty(values)) {
      return;
    }
    String name = "search." + blindex.getName();
    String fields = fields();
    SqlBuilder sb = new SqlBuilder();
    sb.append("with vals(" + fields + ") as (values");
    sb.append(withValues(values));
    sb.append(")");

    sb.append("insert into " + name + "(sid, blindex_id, " + fields + ") select ?, ?, vals.* from vals", sid, blindex.getId());
    jdbcTemplate.update(sb.getSql(), sb.getParams());
  }
}
