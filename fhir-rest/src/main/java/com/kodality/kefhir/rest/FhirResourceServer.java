package com.kodality.kefhir.rest;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.InteractionType;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import com.kodality.kefhir.core.model.search.HistorySearchCriterion;
import com.kodality.kefhir.core.model.search.SearchCriterion;
import com.kodality.kefhir.core.model.search.SearchResult;
import com.kodality.kefhir.core.service.resource.ResourceOperationService;
import com.kodality.kefhir.core.service.resource.ResourceSearchService;
import com.kodality.kefhir.core.service.resource.ResourceService;
import com.kodality.kefhir.core.service.resource.SearchUtil;
import com.kodality.kefhir.core.util.DateUtil;
import com.kodality.kefhir.core.util.ResourceUtil;
import com.kodality.kefhir.rest.interaction.FhirInteraction;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;
import com.kodality.kefhir.rest.util.BundleUtil;
import com.kodality.kefhir.rest.util.PreferredReturn;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import io.micronaut.http.context.ServerRequestContext;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

@Slf4j
@Named("default")
@Singleton
public class FhirResourceServer {
  public static final String DEFAULT = "default";
  @Inject
  protected ResourceService resourceService;
  @Inject
  protected ResourceSearchService resourceSearchService;
  @Inject
  protected ResourceFormatService resourceFormatService;
  @Inject
  protected ResourceOperationService resourceOperationService;

  public String getTargetType() {
    return DEFAULT;
  }

  @FhirInteraction(interaction = InteractionType.READ, mapping = "GET /{}")
  public KefhirResponse read(KefhirRequest req) {
    String resourceId = req.getPath();
    ResourceVersion version = resourceService.load(new VersionId(req.getType(), resourceId));
    if (version.isDeleted()) {
      return new KefhirResponse(410).header("ETag", version.getETag());
    }
    return new KefhirResponse(200, version.getContent())
        .header("Content-Location", uri(version, req))
        .header("Last-Modified", DateUtil.format(version.getModified(), DateUtil.ISO_DATETIME))
        .header("ETag", version.getETag());
  }

  @FhirInteraction(interaction = InteractionType.VREAD, mapping = "GET /{}/_history/{}")
  public KefhirResponse vread(KefhirRequest req) {
    ResourceVersion version = resourceService.load(req.getReference());
    if (version.isDeleted()) {
      return new KefhirResponse(410).header("ETag", version.getETag());
    }
    return new KefhirResponse(200, version.getContent())
        .header("Last-Modified", DateUtil.format(version.getModified(), DateUtil.ISO_DATETIME))
        .header("ETag", version.getETag());
  }

  @FhirInteraction(interaction = InteractionType.CREATE, mapping = "POST /")
  public KefhirResponse create(KefhirRequest req) {
    String ifNoneExist = req.getHeader("If-None-Exist");
    if (ifNoneExist != null) {
      ifNoneExist += "&_count=0";
      SearchCriterion criteria = new SearchCriterion(req.getType(), SearchUtil.parse(ifNoneExist, req.getType()));
      SearchResult result = resourceSearchService.search(criteria);
      if (result.getTotal() == 1) {
        return new KefhirResponse(200);
      }
      if (result.getTotal() > 1) {
        String er = "was expecting 0 or 1 resources. found " + result.getTotal();
        throw new FhirException(412, IssueType.PROCESSING, er);
      }
    }

    ResourceContent content = new ResourceContent(req.getBody(), req.getHeader("Content-Type"));
    ResourceVersion version = resourceService.save(new ResourceId(req.getType()), content, InteractionType.CREATE);
    return created(version, req);
  }


  @FhirInteraction(interaction = InteractionType.UPDATE, mapping = "PUT /{}")
  public KefhirResponse update(KefhirRequest req) {
    String resourceId = req.getPath();
    String contentLocation = req.getHeader("Content-Location");
    Integer ver = contentLocation == null ? null : ResourceUtil.parseReference(contentLocation).getVersion();
    ResourceContent content = new ResourceContent(req.getBody(), req.getHeader("Content-Type"));
    ResourceVersion version = resourceService.save(new VersionId(req.getType(), resourceId, ver), content, InteractionType.UPDATE);
    return version.getId().getVersion() == 1 ? created(version, req) : updated(version, req);
  }

  @FhirInteraction(interaction = InteractionType.UPDATE, mapping = "PUT /")
  public KefhirResponse conditionalUpdate(KefhirRequest req) {
    req.getParameters().put(SearchCriterion._COUNT, Collections.singletonList("1"));
    SearchResult result = resourceSearchService.search(req.getType(), req.getParameters());
    if (result.getTotal() > 1) {
      throw new FhirException(400, IssueType.PROCESSING, "was expecting 1 or 0 resources. found " + result.getTotal());
    }
    String resourceId = result.getTotal() == 1 ? result.getEntries().get(0).getId().getResourceId() : null;
    req.setPath(resourceId);
    return update(req);
  }

  @FhirInteraction(interaction = InteractionType.DELETE, mapping = "DELETE /{}")
  public KefhirResponse delete(KefhirRequest req) {
    resourceService.delete(new ResourceId(req.getType(), req.getPath()));
    return new KefhirResponse(204);
  }

  @FhirInteraction(interaction = InteractionType.HISTORYINSTANCE, mapping = "GET /{}/_history")
  public KefhirResponse history(KefhirRequest req) {
    VersionId id = req.getReference();
    ResourceVersion version = resourceService.load(id);
    if (version == null) {
      throw new FhirException(404, IssueType.NOTFOUND, req.getType() + "/" + id.getResourceId() + " not found");
    }
    HistorySearchCriterion criteria = new HistorySearchCriterion(id.getResourceType(), id.getResourceId());
    criteria.setSince(req.getParameter(HistorySearchCriterion._SINCE));
    List<ResourceVersion> versions = resourceService.loadHistory(criteria);
    return new KefhirResponse(200, BundleUtil.compose(null, versions, BundleType.HISTORY));
  }

  @FhirInteraction(interaction = InteractionType.HISTORYTYPE, mapping = "GET /_history")
  public KefhirResponse historyType(KefhirRequest req) {
    HistorySearchCriterion criteria = new HistorySearchCriterion(req.getType());
    criteria.setSince(req.getParameter(HistorySearchCriterion._SINCE));
    List<ResourceVersion> versions = resourceService.loadHistory(criteria);
    return new KefhirResponse(200, BundleUtil.compose(null, versions, BundleType.HISTORY));
  }

  @FhirInteraction(interaction = InteractionType.SEARCHTYPE, mapping = "GET /")
  public KefhirResponse search(KefhirRequest req) {
    SearchCriterion criteria = new SearchCriterion(req.getType(), SearchUtil.parse(req.getParameters(), req.getType()));
    SearchResult result = resourceSearchService.search(criteria);
    Bundle bundle = BundleUtil.compose(result);
    addPagingLinks(bundle, criteria.getCount(), criteria.getPage(), req);
    return new KefhirResponse(200, bundle);
  }

  @FhirInteraction(interaction = InteractionType.SEARCHTYPE, mapping = "POST /_search")
  public KefhirResponse search_(KefhirRequest req) {
    return search(req);
  }
////  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//  @FhirInteraction(interaction = InteractionType.SEARCHTYPE, path = "POST /_search")


  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "POST /{}/${}")
  public KefhirResponse instanceOperation(KefhirRequest req) {
    String[] p = req.getPath().split("/");
    String resourceId = p[0];
    String operation = p[1];
    if (!operation.startsWith("$")) {
      throw new FhirException(400, IssueType.INVALID, "operation must start with $");
    }
    ResourceId id = new ResourceId(req.getType(), resourceId);
    ResourceContent content = new ResourceContent(req.getBody(), req.getHeader("Content-Type"));
    ResourceContent response = resourceOperationService.runInstanceOperation(operation, id, content);
    return new KefhirResponse(200, response);
  }

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "GET /{}/${}")
  public KefhirResponse instanceOperation_(String resourceId, String operation) {
    throw new FhirException(501, IssueType.NOTSUPPORTED, "GET operation not implemented");
  }

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "POST /${}")
  public KefhirResponse typeOperation(KefhirRequest req) {
    String operation = req.getPath();
    if (!operation.startsWith("$")) {
      throw new FhirException(400, IssueType.INVALID, "operation must start with $");
    }
    ResourceContent content = new ResourceContent(req.getBody(), req.getHeader("Content-Type"));
    ResourceContent response = resourceOperationService.runTypeOperation(operation, req.getType(), content);
    return new KefhirResponse(200, response);
  }

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "GET /${}")
  public KefhirResponse typeOperation_(KefhirRequest req) {
    throw new FhirException(501, IssueType.NOTSUPPORTED, "GET operation not implemented");
  }


  private KefhirResponse created(ResourceVersion version, KefhirRequest req) {
    return new KefhirResponse(201, preferedBody(version, req)).header("Location", uri(version, req));
  }

  private KefhirResponse updated(ResourceVersion version, KefhirRequest req) {
    return new KefhirResponse(200, preferedBody(version, req))
        .header("Content-Location", uri(version, req))
        .header("Last-Modified", DateUtil.format(version.getModified(), DateUtil.ISO_DATETIME));
  }

  private ResourceContent preferedBody(ResourceVersion version, KefhirRequest req) {
    String prefer = PreferredReturn.parse(req.getHeaders());
    if (StringUtils.equals(prefer, PreferredReturn.OperationOutcome)) {
      return resourceFormatService.compose(new OperationOutcome(), "json");
    }
    return version.getContent();
  }

  private String uri(ResourceVersion version, KefhirRequest req) {
    String base = ServerRequestContext.currentRequest().orElseThrow().getUri().toString();
    base = StringUtils.removeEnd(base, req.getType());
    if (version == null) {
      return base;
    }
    return base + version.getReference();
  }

  private void addPagingLinks(Bundle bundle, Integer count, Integer page, KefhirRequest req) {
    if (count == 0) {
      return;
    }
    String pageUrl = ServerRequestContext.currentRequest().orElseThrow().getUri().toString();
    pageUrl = StringUtils.removeEnd(pageUrl, req.getType());

    String queryString = ServerRequestContext.currentRequest().orElseThrow().getUri().getQuery();
    queryString = StringUtils.isEmpty(queryString) ? "" : RegExUtils.removePattern(queryString, "[&?]?_page=[0-9]+");
    pageUrl += StringUtils.isEmpty(queryString) ? "?_page=" : ("?" + queryString + "&_page=");

    bundle.addLink().setRelation("self").setUrl(pageUrl + page);
    bundle.addLink().setRelation("first").setUrl(pageUrl + 1);
    bundle.addLink().setRelation("last").setUrl(pageUrl + (bundle.getTotal() / count + 1));
    if (page > 1) {
      bundle.addLink().setRelation("previous").setUrl(pageUrl + (page - 1));
    }
    if (page * count < bundle.getTotal()) {
      bundle.addLink().setRelation("next").setUrl(pageUrl + (page + 1));
    }
  }
}

