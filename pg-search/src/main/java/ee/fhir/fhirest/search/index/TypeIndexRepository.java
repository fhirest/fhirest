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
