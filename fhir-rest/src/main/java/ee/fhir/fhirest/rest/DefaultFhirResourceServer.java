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

package ee.fhir.fhirest.rest;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.InteractionType;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.HistorySearchCriterion;
import ee.fhir.fhirest.core.model.search.SearchCriterion;
import ee.fhir.fhirest.core.model.search.SearchCriterionBuilder;
import ee.fhir.fhirest.core.model.search.SearchResult;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.core.service.resource.ResourceOperationService;
import ee.fhir.fhirest.core.service.resource.ResourceSearchService;
import ee.fhir.fhirest.core.service.resource.ResourceService;
import ee.fhir.fhirest.core.util.DateUtil;
import ee.fhir.fhirest.core.util.ResourceUtil;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.rest.model.FhirestResponse;
import ee.fhir.fhirest.rest.operation.OperationParametersReader;
import ee.fhir.fhirest.rest.util.BundleUtil;
import ee.fhir.fhirest.structure.api.ResourceContent;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Default http request consumer for all resource types.</p>
 */
@Slf4j
@Primary
@Component
public class DefaultFhirResourceServer extends BaseFhirResourceServer {
  public static final String DEFAULT = "default";
  @Inject
  protected ResourceService resourceService;
  @Inject
  protected ResourceSearchService resourceSearchService;
  @Inject
  protected OperationParametersReader operationParametersReader;
  @Inject
  protected ResourceOperationService resourceOperationService;

  @Override
  public String getTargetType() {
    return DEFAULT;
  }

  @Override
  public FhirestResponse read(FhirestRequest req) {
    String resourceId = req.getPath();
    ResourceVersion version = resourceService.load(new VersionId(req.getType(), resourceId));
    if (version.isDeleted()) {
      return new FhirestResponse(410).header("ETag", version.getETag());
    }
    return new FhirestResponse(200, version.getContent())
        .header("Content-Location", uri(version, req))
        .header("Last-Modified", DateUtil.format(version.getModified(), DateUtil.ISO_DATETIME))
        .header("ETag", version.getETag());
  }

  @Override
  public FhirestResponse vread(FhirestRequest req) {
    ResourceVersion version = resourceService.load(req.getReference());
    if (version.isDeleted()) {
      return new FhirestResponse(410).header("ETag", version.getETag());
    }
    return new FhirestResponse(200, version.getContent())
        .header("Last-Modified", DateUtil.format(version.getModified(), DateUtil.ISO_DATETIME))
        .header("ETag", version.getETag());
  }

  @Override
  public FhirestResponse create(FhirestRequest req) {
    String ifNoneExist = req.getHeader("If-None-Exist");
    if (ifNoneExist != null) {
      SearchCriterion criteria = SearchCriterionBuilder.parse(ifNoneExist + "&_count=0", req.getType());
      SearchResult result = resourceSearchService.search(criteria);
      if (result.getTotal() == 1) {
        return new FhirestResponse(200);
      }
      if (result.getTotal() > 1) {
        throw new FhirException(FhirestIssue.FEST_002, "uri", ifNoneExist, "total", result.getTotal());
      }
    }
    ResourceContent content = new ResourceContent(req.getBody(), req.getContentTypeName());
    ResourceVersion version = resourceService.save(new ResourceId(req.getType()), content, InteractionType.CREATE);
    return created(version, req);
  }

  @Override
  public FhirestResponse update(FhirestRequest req) {
    String resourceId = req.getPath();
    String contentLocation = req.getHeader("Content-Location");
    Integer ver = contentLocation == null ? null : ResourceUtil.parseReference(contentLocation).getVersion();
    ResourceContent content = new ResourceContent(req.getBody(), req.getContentTypeName());
    boolean exists = resourceId != null && exists(req.getType(), resourceId);
    if (!exists && !isUpdateCreateAllowed(req.getType()) && !"POST".equals(req.getTransactionMethod())) {
      throw new FhirException(FhirestIssue.FEST_006);
    }
    ResourceVersion version = resourceService.save(new VersionId(req.getType(), resourceId, ver), content, InteractionType.UPDATE);
    return exists ? updated(version, req) : created(version, req);
  }

  protected boolean exists(String type, String id) {
    if (ConformanceHolder.getSearchParam(type, "_id") == null) {
      return !resourceService.load(List.of(new VersionId(type, id))).isEmpty();
    }
    return resourceSearchService.search(type, "_id", id, "_count", "0").getTotal() > 0;
  }

  private boolean isUpdateCreateAllowed(String type) {
    CapabilityStatementRestResourceComponent res = ConformanceHolder.getCapabilityResource(type);
    return !res.hasUpdateCreate() || res.getUpdateCreate();
    // empty updateCreate = allowed. should remove this at some point. added for backwards compatibility, when this setting did not exist
  }

  @Override
  public FhirestResponse conditionalUpdate(FhirestRequest req) {
    if (req.getParameters().isEmpty()) {
      throw new FhirException(FhirestIssue.FEST_007);
    }
    req.getParameters().put(SearchCriterion._COUNT, Collections.singletonList("1"));
    SearchResult result = resourceSearchService.search(req.getType(), req.getParameters());
    if (result.getTotal() > 1) {
      throw new FhirException(FhirestIssue.FEST_002, "uri", req.getParametersString(), "total", result.getTotal());
    }
    String resourceId = result.getTotal() == 1 ? result.getEntries().get(0).getId().getResourceId() : null;
    req.setPath(resourceId);
    return update(req);
  }

  @Override
  public FhirestResponse delete(FhirestRequest req) {
    resourceService.delete(new ResourceId(req.getType(), req.getPath()));
    return new FhirestResponse(204);
  }

  @Override
  public FhirestResponse history(FhirestRequest req) {
    VersionId id = req.getReference();
    boolean exists = exists(req.getType(), id.getResourceId());
    if (!exists) {
      throw new FhirException(FhirestIssue.FEST_008, "resource", req.getType() + "/" + id.getResourceId());
    }
    HistorySearchCriterion criteria = new HistorySearchCriterion(id.getResourceType(), id.getResourceId());
    criteria.setSince(req.getParameter(HistorySearchCriterion._SINCE));
    criteria.setCount(req.getParameter(HistorySearchCriterion._COUNT));
    List<ResourceVersion> versions = resourceService.loadHistory(criteria);
    return new FhirestResponse(200, BundleUtil.compose(null, versions, BundleType.HISTORY));
  }

  @Override
  public FhirestResponse historyType(FhirestRequest req) {
    HistorySearchCriterion criteria = new HistorySearchCriterion(req.getType());
    criteria.setSince(req.getParameter(HistorySearchCriterion._SINCE));
    criteria.setCount(req.getParameter(HistorySearchCriterion._COUNT));
    List<ResourceVersion> versions = resourceService.loadHistory(criteria);
    return new FhirestResponse(200, BundleUtil.compose(null, versions, BundleType.HISTORY));
  }

  @Override
  public FhirestResponse search(FhirestRequest req) {
    SearchCriterion criteria = SearchCriterionBuilder.parse(req.getParameters(), req.getType());
    SearchResult result = resourceSearchService.search(criteria);
    Bundle bundle = BundleUtil.compose(result);
    addPagingLinks(bundle, criteria.getCount(), criteria.getPage(), req);
    return new FhirestResponse(200, bundle);
  }

  @Override
  public FhirestResponse search_(FhirestRequest req) {
    return search(req);
  }

  @Override
  public FhirestResponse searchCompartment(FhirestRequest req) {
    String[] p = req.getPath().split("/");
    String id = p[0];
    String compartment = p[1];
    List<String> compartmentParams = ConformanceHolder.getCompartmentParam(req.getType(), compartment);
    if (CollectionUtils.isEmpty(compartmentParams)) {
      throw new FhirException(FhirestIssue.FEST_009, "compartment", compartment, "resource", req.getType());
    }
    if (compartmentParams.size() > 1) {
      throw new FhirException(FhirestIssue.FEST_001, "desc", "multiple compartment params not yet supported");
    }
    Map<String, List<String>> query = new HashMap<>(req.getParameters());
    query.put(compartmentParams.get(0), List.of(id));

    SearchCriterion criteria = SearchCriterionBuilder.parse(query, compartment);
    SearchResult result = resourceSearchService.search(criteria);
    Bundle bundle = BundleUtil.compose(result);
    addPagingLinks(bundle, criteria.getCount(), criteria.getPage(), req);
    return new FhirestResponse(200, bundle);
  }

  @Override
  public FhirestResponse instanceOperation(FhirestRequest req) {
    String[] p = req.getPath().split("/");
    String resourceId = p[0];
    String operation = p[1];
    if (!operation.startsWith("$")) {
      throw new FhirException(FhirestIssue.FEST_010);
    }
    ResourceId id = new ResourceId(req.getType(), resourceId);

    ResourceContent content = operationParametersReader.readOperationParameters(operation, req);
    ResourceContent response = resourceOperationService.runInstanceOperation(operation, id, content);
    return new FhirestResponse(200, response);
  }

  @Override
  public FhirestResponse instanceOperation_(FhirestRequest req) {
    return instanceOperation(req);
  }

  @Override
  public FhirestResponse typeOperation(FhirestRequest req) {
    String operation = req.getPath();
    if (!operation.startsWith("$")) {
      throw new FhirException(FhirestIssue.FEST_010);
    }
    ResourceContent content = operationParametersReader.readOperationParameters(operation, req);
    ResourceContent response = resourceOperationService.runTypeOperation(operation, req.getType(), content);
    return new FhirestResponse(200, response);
  }


  @Override
  public FhirestResponse typeOperation_(FhirestRequest req) {
    return typeOperation(req);
  }
}

