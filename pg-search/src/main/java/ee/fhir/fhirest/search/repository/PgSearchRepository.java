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

import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.core.model.search.SearchCriterion;
import ee.fhir.fhirest.search.api.PgResourceSearchFilter;
import ee.fhir.fhirest.search.model.Blindex;
import ee.fhir.fhirest.search.sql.SearchSqlUtil;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
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

  // Canonical-only RelatedArtifact params in R5 (use URI index)
  // NOTE: deliberately excludes "depends-on" (mixed)
  private static final java.util.Set<String> CANONICAL_REF_PARAMS =
      java.util.Set.of("predecessor", "successor", "derived-from", "composed-of");

  // Mixed (union) param in R5: canonical .resource + reference .library
  private static final java.util.Set<String> DUAL_URI_OR_REF =
      java.util.Set.of("depends-on");

  public Integer count(SearchCriterion criteria) {
    SqlBuilder sb = new SqlBuilder("SELECT count(1) FROM search.resource base ");
    sb.append(joins(criteria));
    sb.append(" WHERE base.resource_type = ?", ResourceStructureRepository.getTypeId(criteria.getType()));
    sb.append(criteria(criteria));
    pgResourceSearchFilter.ifPresent(f -> f.filter(sb, "base"));
    log.debug(sb.getPretty());
    return jdbcTemplate.queryForObject(sb.getSql(), Integer.class, sb.getParams());
  }

  private QueryParam maybeFlipToUri(QueryParam p) {
    // Only flip well-known RelatedArtifact-based params when they were declared as REFERENCE
    // Skip mixed (depends-on) so we can build both branches
    if (p.getType() == SearchParamType.REFERENCE
        && CANONICAL_REF_PARAMS.contains(p.getKey())) {
      QueryParam q = new QueryParam(p.getKey(), p.getModifier(), SearchParamType.URI, p.getResourceType());
      // Important: add chains BEFORE setting values (addChain asserts values==null)
      if (p.getChains() != null) {
        for (QueryParam c : p.getChains()) {
          q.addChain(c);
        }
      }
      if (p.getValues() != null) {
        q.setValues(p.getValues()); // will propagate to chains if present
      }
      return q;
    }
    return p;
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
      SqlBuilder peanut;

      // Special handling for mixed canonical+reference params (e.g., Measure:depends-on)
      if (param.getType() == SearchParamType.REFERENCE && DUAL_URI_OR_REF.contains(param.getKey())) {
        SqlBuilder uriExpr = buildDependsOnUriExists(criteria.getType(), param, "base");
        SqlBuilder refExpr = buildDependsOnRefExists(criteria.getType(), param, "base");

        if (uriExpr != null && refExpr != null) {
          peanut = new SqlBuilder().append("(").append(uriExpr).append(" OR ").append(refExpr).append(")");
        } else {
          peanut = (uriExpr != null) ? uriExpr : refExpr;
        }
      } else {
        // Normal path, including canonical-only params (flip to URI when appropriate)
        QueryParam effective = maybeFlipToUri(param);
        peanut = SearchSqlUtil.condition(effective, "base");
      }

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

  // --- helpers: build clean EXISTS for depends-on without UNION mixing ---

  private SqlBuilder buildDependsOnUriExists(String resourceType, QueryParam param, String baseAlias) {
    List<Blindex> uriIdx = BlindexRepository.getIndexes(resourceType).stream()
        .filter(b -> "uri".equalsIgnoreCase(b.getParamType()))
        .filter(b -> containsDependsOnCanonicalPath(b.getPath()))
        .collect(Collectors.toList());
    if (uriIdx.isEmpty()) return null;
  
    String raw = firstValue(param);
    if (raw == null) return null;
    String canonical = raw.replace("%7C", "|").replace("%7c", "|");
  
    SqlBuilder out = new SqlBuilder();
    boolean first = true;
    for (Blindex b : uriIdx) {
      if (!first) out.append(" OR ");
      first = false;
      out.append("EXISTS (SELECT 1 FROM search.")
         .append(b.getName())
         .append(" i WHERE i.active = true and i.sid = ")
         .append(baseAlias)
         .append(".sid AND i.uri = ?)", canonical);
    }
    return uriIdx.size() > 1 ? new SqlBuilder().append("(").append(out).append(")") : out;
  }

  private SqlBuilder buildDependsOnRefExists(String resourceType, QueryParam param, String baseAlias) {
    List<Blindex> refIdx = BlindexRepository.getIndexes(resourceType).stream()
        .filter(b -> "reference".equalsIgnoreCase(b.getParamType()))
        .filter(b -> containsDependsOnLibraryPath(b.getPath()))
        .collect(Collectors.toList());
    if (refIdx.isEmpty()) return null;
  
    String raw = firstValue(param);
    if (raw == null) return null;
    int slash = raw.indexOf('/');
    String rType = (slash > 0) ? raw.substring(0, slash) : "";
    String rId   = (slash > 0) ? raw.substring(slash + 1) : raw;
  
    SqlBuilder out = new SqlBuilder();
    boolean first = true;
    for (Blindex b : refIdx) {
      if (!first) out.append(" OR ");
      first = false;
      out.append("EXISTS (SELECT 1 FROM search.")
         .append(b.getName())
         .append(" i WHERE i.active = true and i.sid = ")
         .append(baseAlias)
         .append(".sid AND i.id = ? AND i.type_id = search.rt_id(?))", rId, rType);
    }
    return refIdx.size() > 1 ? new SqlBuilder().append("(").append(out).append(")") : out;
  }

  private static boolean containsDependsOnCanonicalPath(String path) {
    // Matches: Measure.relatedArtifact.where(type='depends-on').resource
    return path != null
        && path.contains("relatedArtifact.where(type='depends-on')")
        && path.endsWith(".resource");
  }

  private static boolean containsDependsOnLibraryPath(String path) {
    // Matches: Measure.library (allow either exact or suffix)
    return path != null
        && (path.equals("Measure.library") || path.endsWith(".library"));
  }

  private static String firstValue(QueryParam p) {
    List<String> vs = p.getValues();
    return (vs == null || vs.isEmpty()) ? null : vs.get(0);
  }
}
