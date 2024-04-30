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

package ee.fhir.fhirest.search.repository;

import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.core.model.search.SearchCriterion;
import ee.fhir.fhirest.search.api.PgResourceSearchFilter;
import ee.fhir.fhirest.search.sql.SearchSqlUtil;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PgSearchRepository {
  @Inject
  @Named("searchAppJdbcTemplate")
  private JdbcTemplate jdbcTemplate;
  @Inject
  private Optional<PgResourceSearchFilter> pgResourceSearchFilter;

  public Integer count(SearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder("SELECT count(1) FROM search.resource base ");
    sb.append(joins(criteria));
    sb.append(" WHERE base.resource_type = ?", ResourceStructureRepository.getTypeId(criteria.getType()));
    sb.append(criteria(criteria));
    pgResourceSearchFilter.ifPresent(f -> f.filter(sb, "base"));
    log.debug(sb.getPretty());
    return jdbcTemplate.queryForObject(sb.getSql(), Integer.class, sb.getParams());
  }

  public List<ResourceId> search(SearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder("SELECT base.resource_id, rt.type resource_type FROM search.resource base ");
    sb.append(" INNER JOIN search.resource_type rt on rt.id = base.resource_type ");
    sb.append(joins(criteria));
    sb.append(" WHERE base.resource_type = ?", ResourceStructureRepository.getTypeId(criteria.getType()));
    sb.append(criteria(criteria));
    pgResourceSearchFilter.ifPresent(f -> f.filter(sb, "base"));
    sb.append(order(criteria));
    sb.append(limit(criteria));
    log.debug(sb.getPretty());
    return jdbcTemplate.query(sb.getSql(), (rs, x) -> new ResourceId(rs.getString("resource_type"), rs.getString("resource_id")), sb.getParams());
  }

  private SqlBuilder limit(SearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder();
    Integer limit = criteria.getCount();
    Integer page = criteria.getPage();
    Integer offset = limit * (page - 1);
    return sb.append(" LIMIT ? OFFSET ?", limit, offset);
  }

  private SqlBuilder joins(SearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder();
    sb.append(SearchSqlUtil.chain(criteria.getChains(), "base"));
    return sb;
  }

  private SqlBuilder criteria(SearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder();
    for (QueryParam param : criteria.getConditions()) {
      SqlBuilder peanut = SearchSqlUtil.condition(param, "base");
      if (peanut != null) {
        sb.and("(").append(peanut).append(")");
      }
    }
    sb.and("base.active = true");
    return sb;
  }

  private SqlBuilder order(SearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder();
    boolean first = true;
    for (QueryParam param : criteria.getResultParams(SearchCriterion._SORT)) {
      SqlBuilder sql = SearchSqlUtil.order(param, "base");
      if (sql == null) {
        continue;
      }
      sb.append(first ? " ORDER BY " : ",");
      sb.append(sql);
      first = false;
    }
    return sb;
  }

}
