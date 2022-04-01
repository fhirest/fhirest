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
package com.kodality.kefhir.store.repository;

import com.google.common.collect.Lists;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import com.kodality.kefhir.core.model.search.HistorySearchCriterion;
import com.kodality.kefhir.core.util.DateUtil;
import com.kodality.kefhir.core.util.JsonUtil;
import com.kodality.kefhir.store.api.PgResourceFilter;
import com.kodality.kefhir.util.sql.SqlBuilder;
import io.micronaut.context.annotation.Primary;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static java.util.stream.Collectors.joining;

@Primary
@Singleton
public class ResourceRepository {
  private final JdbcTemplate jdbcTemplate;
  @Inject
  private Optional<PgResourceFilter> pgResourceFilter;

  public ResourceRepository(@Named("storeAppJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public String getNextResourceId() {
    //TODO: may already exist
    return String.valueOf(jdbcTemplate.queryForObject("select nextval('store.resource_id_seq')", Long.class));
  }

  public void create(ResourceVersion version) {
    if (version.getId().getResourceId() == null) {
      version.getId().setResourceId(getNextResourceId());
    }
    String sql = "INSERT INTO store.resource (type, id, version, author, content, sys_status) VALUES (?,?,?,?::jsonb,?::jsonb,?)";
    jdbcTemplate.update(sql,
        version.getId().getResourceType(),
        version.getId().getResourceId(),
        version.getId().getVersion(),
        JsonUtil.toJson(version.getAuthor()),
        version.getContent() == null ? null : version.getContent().getValue(),
        version.isDeleted() ? "C" : "A");
  }

  public Integer getLastVersion(ResourceId id) {
    String sql = "SELECT COALESCE(max(version),0) FROM store.resource WHERE type = ? AND id = ?";
    return jdbcTemplate.queryForObject(sql, Integer.class, id.getResourceType(), id.getResourceId());
  }

  public List<ResourceVersion> load(List<ResourceId> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return List.of();
    }
    return Lists.partition(ids, 100).stream().flatMap(pids -> {
      SqlBuilder sb = new SqlBuilder();
      sb.append("SELECT * FROM store.resource r WHERE sys_status = 'A'");
      sb.append(" and (type, id) in (").append(pids.stream().map(id -> "(?,?)").collect(joining(","))).append(")");
      pids.forEach(id -> sb.add(id.getResourceType(), id.getResourceId()));
      return jdbcTemplate.query(sb.getSql(), new ResourceRowMapper(), sb.getParams()).stream();
    }).collect(Collectors.toList());
  }

  public ResourceVersion load(VersionId id) {
    SqlBuilder sb = new SqlBuilder();
    sb.append("SELECT * FROM store.resource r WHERE type = ? AND id = ?", id.getResourceType(), id.getResourceId());
    if (id.getVersion() != null) {
      sb.append(" AND version = ?", id.getVersion());
    } else {
      sb.append(" AND sys_status = 'A'");
    }
    pgResourceFilter.ifPresent(f -> f.filter(sb, "r"));
    try {
      return jdbcTemplate.queryForObject(sb.getSql(), new ResourceRowMapper(), sb.getParams());
    } catch (IncorrectResultSizeDataAccessException e) {
      if (e.getActualSize() == 0) {
        return null;
      }
      throw e;
    }
  }

  public List<ResourceVersion> loadHistory(HistorySearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder();
    sb.append("SELECT * FROM store.resource r WHERE 1=1");
    sb.appendIfNotNull(" AND type = ?", criteria.getResourceType());
    sb.appendIfNotNull(" AND id = ?", criteria.getResourceId());
    if (criteria.getSince() != null) {
      sb.append(" AND updated >= ?", DateUtil.parse(criteria.getSince()));
    }
    pgResourceFilter.ifPresent(f -> f.filter(sb, "r"));
    sb.append(" ORDER BY updated desc");
    return jdbcTemplate.query(sb.getSql(), new ResourceRowMapper(), sb.getParams());
  }

}
