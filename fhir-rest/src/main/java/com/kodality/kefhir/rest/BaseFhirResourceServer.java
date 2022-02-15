package com.kodality.kefhir.rest;

import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.util.DateUtil;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;
import com.kodality.kefhir.rest.util.PreferredReturn;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import io.micronaut.http.context.ServerRequestContext;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

public abstract class BaseFhirResourceServer implements FhirResourceServer {
  @Override
  public abstract String getTargetType();

  @Override
  public KefhirResponse read(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse vread(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse create(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse update(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse conditionalUpdate(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse delete(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse history(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse historyType(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse search(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse search_(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse instanceOperation(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse instanceOperation_(String resourceId, String operation) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse typeOperation(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }

  @Override
  public KefhirResponse typeOperation_(KefhirRequest req) {
    throw new FhirException(400, IssueType.NOTSUPPORTED, "not supported");
  }


  protected KefhirResponse created(ResourceVersion version, KefhirRequest req) {
    return new KefhirResponse(201, preferedBody(version, req)).header("Location", uri(version, req));
  }

  protected KefhirResponse updated(ResourceVersion version, KefhirRequest req) {
    return new KefhirResponse(200, preferedBody(version, req))
        .header("Content-Location", uri(version, req))
        .header("Last-Modified", DateUtil.format(version.getModified(), DateUtil.ISO_DATETIME));
  }

  protected ResourceContent preferedBody(ResourceVersion version, KefhirRequest req) {
    String prefer = PreferredReturn.parse(req.getHeaders());
    if (StringUtils.equals(prefer, PreferredReturn.OperationOutcome)) {
      return ResourceFormatService.get().compose(new OperationOutcome(), "json");
    }
    if (StringUtils.equals(prefer, PreferredReturn.representation)) {
      return version.getContent();
    }
    return null;
  }

  protected String uri(ResourceVersion version, KefhirRequest req) {
    return version.getReference();
  }

  protected void addPagingLinks(Bundle bundle, Integer count, Integer page, KefhirRequest req) {
    if (count == 0) {
      return;
    }
    String pageUrl = ServerRequestContext.currentRequest().orElseThrow().getPath();
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

