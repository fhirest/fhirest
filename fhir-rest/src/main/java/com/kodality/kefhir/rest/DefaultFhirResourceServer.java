package com.kodality.kefhir.rest;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.InteractionType;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import com.kodality.kefhir.core.model.search.HistorySearchCriterion;
import com.kodality.kefhir.core.model.search.SearchCriterion;
import com.kodality.kefhir.core.model.search.SearchResult;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.core.service.resource.ResourceOperationService;
import com.kodality.kefhir.core.service.resource.ResourceSearchService;
import com.kodality.kefhir.core.service.resource.ResourceService;
import com.kodality.kefhir.core.service.resource.SearchUtil;
import com.kodality.kefhir.core.util.DateUtil;
import com.kodality.kefhir.core.util.ResourceUtil;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;
import com.kodality.kefhir.rest.util.BundleUtil;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

@Slf4j
@Named("default")
@Singleton
public class DefaultFhirResourceServer extends BaseFhirResourceServer {
  public static final String DEFAULT = "default";
  @Inject
  protected ResourceService resourceService;
  @Inject
  protected ResourceSearchService resourceSearchService;
  @Inject
  protected ResourceFormatService resourceFormatService;
  @Inject
  protected ResourceOperationService resourceOperationService;

  @Override
  public String getTargetType() {
    return DEFAULT;
  }

  @Override
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

  @Override
  public KefhirResponse vread(KefhirRequest req) {
    ResourceVersion version = resourceService.load(req.getReference());
    if (version.isDeleted()) {
      return new KefhirResponse(410).header("ETag", version.getETag());
    }
    return new KefhirResponse(200, version.getContent())
        .header("Last-Modified", DateUtil.format(version.getModified(), DateUtil.ISO_DATETIME))
        .header("ETag", version.getETag());
  }

  @Override
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
    ResourceContent content = new ResourceContent(req.getBody(), req.getContentTypeName());
    ResourceVersion version = resourceService.save(new ResourceId(req.getType()), content, InteractionType.CREATE);
    return created(version, req);
  }

  @Override
  public KefhirResponse update(KefhirRequest req) {
    String resourceId = req.getPath();
    String contentLocation = req.getHeader("Content-Location");
    Integer ver = contentLocation == null ? null : ResourceUtil.parseReference(contentLocation).getVersion();
    ResourceContent content = new ResourceContent(req.getBody(), req.getContentTypeName());
    boolean exists = resourceId != null && resourceSearchService.search(req.getType(), "_id", resourceId, "_count", "0").getTotal() > 0;
    if (!exists && ConformanceHolder.getCapabilityResource(req.getType()).getUpdateCreate()) {
      throw new FhirException(400, IssueType.NOTSUPPORTED, "create on update is disabled by conformance");
    }
    ResourceVersion version = resourceService.save(new VersionId(req.getType(), resourceId, ver), content, InteractionType.UPDATE);
    return exists ? updated(version, req) : created(version, req);
  }

  @Override
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

  @Override
  public KefhirResponse delete(KefhirRequest req) {
    resourceService.delete(new ResourceId(req.getType(), req.getPath()));
    return new KefhirResponse(204);
  }

  @Override
  public KefhirResponse history(KefhirRequest req) {
    VersionId id = req.getReference();
    ResourceVersion version = resourceService.load(id);
    if (version == null) {
      throw new FhirException(404, IssueType.NOTFOUND, req.getType() + "/" + id.getResourceId() + " not found");
    }
    HistorySearchCriterion criteria = new HistorySearchCriterion(id.getResourceType(), id.getResourceId());
    criteria.setSince(req.getParameter(HistorySearchCriterion._SINCE));
    criteria.setCount(req.getParameter(HistorySearchCriterion._COUNT));
    List<ResourceVersion> versions = resourceService.loadHistory(criteria);
    return new KefhirResponse(200, BundleUtil.compose(null, versions, BundleType.HISTORY));
  }

  @Override
  public KefhirResponse historyType(KefhirRequest req) {
    HistorySearchCriterion criteria = new HistorySearchCriterion(req.getType());
    criteria.setSince(req.getParameter(HistorySearchCriterion._SINCE));
    criteria.setCount(req.getParameter(HistorySearchCriterion._COUNT));
    List<ResourceVersion> versions = resourceService.loadHistory(criteria);
    return new KefhirResponse(200, BundleUtil.compose(null, versions, BundleType.HISTORY));
  }

  @Override
  public KefhirResponse search(KefhirRequest req) {
    SearchCriterion criteria = new SearchCriterion(req.getType(), SearchUtil.parse(req.getParameters(), req.getType()));
    SearchResult result = resourceSearchService.search(criteria);
    Bundle bundle = BundleUtil.compose(result);
    addPagingLinks(bundle, criteria.getCount(), criteria.getPage(), req);
    return new KefhirResponse(200, bundle);
  }

  @Override
  public KefhirResponse search_(KefhirRequest req) {
    return search(req);
  }

  @Override
  public KefhirResponse searchCompartment(KefhirRequest req) {
    String[] p = req.getPath().split("/");
    String id = p[0];
    String compartment = p[1];
    List<String> compartmentParams = ConformanceHolder.getCompartmentParam(req.getType(), compartment);
    if (CollectionUtils.isEmpty(compartmentParams)) {
      throw new FhirException(400, IssueType.INVALID, "unknown compartment " + compartment + " for " + req.getType());
    }
    if (compartmentParams.size() > 1) {
      throw new FhirException(400, IssueType.NOTSUPPORTED, "multiple compartment params not yet supported");
    }
    Map<String, List<String>> query = new HashMap<>(req.getParameters());
    query.put(compartmentParams.get(0), List.of(id));

    SearchCriterion criteria = new SearchCriterion(compartment, SearchUtil.parse(query, compartment));
    SearchResult result = resourceSearchService.search(criteria);
    Bundle bundle = BundleUtil.compose(result);
    addPagingLinks(bundle, criteria.getCount(), criteria.getPage(), req);
    return new KefhirResponse(200, bundle);
  }

  @Override
  public KefhirResponse instanceOperation(KefhirRequest req) {
    String[] p = req.getPath().split("/");
    String resourceId = p[0];
    String operation = p[1];
    if (!operation.startsWith("$")) {
      throw new FhirException(400, IssueType.INVALID, "operation must start with $");
    }
    ResourceId id = new ResourceId(req.getType(), resourceId);
    ResourceContent content = new ResourceContent(req.getBody(), req.getContentTypeName());
    ResourceContent response = resourceOperationService.runInstanceOperation(operation, id, content);
    return new KefhirResponse(200, response);
  }

  @Override
  public KefhirResponse instanceOperation_(KefhirRequest req) {
    return instanceOperation(req);
  }

  @Override
  public KefhirResponse typeOperation(KefhirRequest req) {
    String operation = req.getPath();
    if (!operation.startsWith("$")) {
      throw new FhirException(400, IssueType.INVALID, "operation must start with $");
    }
    ResourceContent content = new ResourceContent(req.getBody(), req.getContentTypeName());
    ResourceContent response = resourceOperationService.runTypeOperation(operation, req.getType(), content);
    return new KefhirResponse(200, response);
  }

  @Override
  public KefhirResponse typeOperation_(KefhirRequest req) {
    return typeOperation(req);
  }
}

