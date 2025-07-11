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

package ee.fhir.fhirest.store.repository;

import com.google.common.collect.Lists;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.HistorySearchCriterion;
import ee.fhir.fhirest.core.util.JsonUtil;
import ee.fhir.fhirest.store.api.PgResourceFilter;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Primary
@Component
public class ResourceRepository {
  private final JdbcTemplate jdbcTemplate;
  @Inject
  private Optional<PgResourceFilter> pgResourceFilter;

  public ResourceRepository(@Named("storeAppJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public String getNextResourceId() {
    //TODO: may already exist
    return String.valueOf(jdbcTemplate.queryForObject("SELECT nextval('store.resource_id_seq')", Long.class));
  }

  public void create(ResourceVersion version, List<String> profiles) {
    if (version.getId().getResourceId() == null) {
      version.getId().setResourceId(getNextResourceId());
    }
    String sql = "INSERT INTO store.resource (type, id, version, author, content, profiles, sys_status) VALUES (?,?,?,?::jsonb,?::jsonb, ?::text[], ?)";
    jdbcTemplate.update(sql,
        version.getId().getResourceType(),
        version.getId().getResourceId(),
        version.getId().getVersion(),
        JsonUtil.toJson(version.getAuthor()),
        version.getContent() == null ? null : version.getContent().getValue(),
        CollectionUtils.isEmpty(profiles) ? null : "{" + String.join(",", profiles) + "}",
        version.isDeleted() ? "C" : "A");
  }

  public Integer getLastVersion(ResourceId id) {
    String sql = "SELECT COALESCE(max(version),0) FROM store.resource WHERE type = ? AND id = ?";
    return jdbcTemplate.queryForObject(sql, Integer.class, id.getResourceType(), id.getResourceId());
  }

  public List<ResourceVersion> load(List<VersionId> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return List.of();
    }
    List<VersionId> unversioned = ids.stream().filter(id -> id.getVersion() == null).toList();
    List<VersionId> versioned = ids.stream().filter(id -> id.getVersion() != null).toList();
    return Stream.concat(
        Lists.partition(unversioned, 100).stream().flatMap(pids -> {
          SqlBuilder sb = new SqlBuilder();
          sb.append("SELECT * FROM store.resource r WHERE sys_status = 'A'");
          sb.append(" and (type, id) in (").append(pids.stream().map(id -> "(?,?)").collect(joining(","))).append(")");
          pids.forEach(id -> sb.add(id.getResourceType(), id.getResourceId()));
          return jdbcTemplate.query(sb.getSql(), new ResourceRowMapper(), sb.getParams()).stream();
        }),
        Lists.partition(versioned, 100).stream().flatMap(pids -> {
          SqlBuilder sb = new SqlBuilder();
          sb.append("SELECT * FROM store.resource r WHERE");
          sb.append(" (type, id, version) in (").append(pids.stream().map(id -> "(?,?,?)").collect(joining(","))).append(")");
          pids.forEach(id -> sb.add(id.getResourceType(), id.getResourceId(), id.getVersion()));
          return jdbcTemplate.query(sb.getSql(), new ResourceRowMapper(), sb.getParams()).stream();
        })
    ).collect(Collectors.toList());
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
      sb.append(" AND updated >= ?", criteria.getSince());
    }
    pgResourceFilter.ifPresent(f -> f.filter(sb, "r"));
    sb.append(" ORDER BY updated desc");
    sb.append(limit(criteria));
    return jdbcTemplate.query(sb.getSql(), new ResourceRowMapper(), sb.getParams());
  }

  private SqlBuilder limit(HistorySearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder();
    Integer limit = criteria.getCount();
//    Integer page = criteria.getPage();
//    Integer offset = limit * (page - 1);
    Integer offset = 0;
    return sb.append(" LIMIT ? OFFSET ?", limit, offset);
  }

}
