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

package ee.fhir.fhirest.rest.bundle;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.search.SearchCriterion;
import ee.fhir.fhirest.core.model.search.SearchCriterionBuilder;
import ee.fhir.fhirest.core.model.search.SearchResult;
import ee.fhir.fhirest.core.service.resource.ResourceSearchService;
import ee.fhir.fhirest.rest.FhirestEndpointService;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.rest.model.FhirestResponse;
import ee.fhir.fhirest.rest.util.PreferredReturn;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import ee.fhir.fhirest.tx.TransactionService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.Bundle.HTTPVerb;
import org.hl7.fhir.r5.model.Bundle.LinkRelationTypes;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.UriType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BundleService implements BundleSaveHandler {
  private final ResourceSearchService searchService;
  private final BundleReferenceHandler bundleReferenceHandler;
  private final FhirestEndpointService endpointService;
  private final ResourceFormatService resourceFormatService;
  private final TransactionService tx;

  @Override
  public Bundle save(Bundle bundle, String prefer) {
    if (bundle.getEntry().stream().anyMatch(e -> !e.hasRequest())) {
      throw new FhirException(FhirestIssue.FEST_025);
    }

    if (bundle.getType() == BundleType.BATCH) {
      bundle.getEntry().sort(new EntityMethodOrderComparator());
      return batch(bundle, prefer);
    }
    if (bundle.getType() == BundleType.TRANSACTION) {
      validateTransaction(bundle);
      bundleReferenceHandler.replaceIds(bundle);
      bundle.getEntry().sort(new EntityMethodOrderComparator()); //moved after replaceIds because incorrect behavior in case of conditional updates
      return transaction(bundle, prefer);
    }
    throw new FhirException(FhirestIssue.FEST_026);
  }

  private void validateTransaction(Bundle bundle) {
    bundle.getEntry().forEach(entry -> {
      if (entry.getRequest().getMethod() == HTTPVerb.POST && entry.getRequest().getIfNoneExist() != null) {
        String ifNoneExist = entry.getRequest().getIfNoneExist() + "&_count=0";
        String type = entry.getResource().getResourceType().name();
        SearchCriterion criteria = SearchCriterionBuilder.parse(ifNoneExist, type);
        SearchResult result = searchService.search(criteria);
        if (result.getTotal() == 1) {
          entry.getRequest().setMethod(HTTPVerb.NULL); // ignore
        }
        if (result.getTotal() > 1) {
          throw new FhirException(FhirestIssue.FEST_002, "uri", entry.getRequest().getIfNoneExist(), "total", result.getTotal());
        }
      }
    });
  }

  private Bundle batch(Bundle bundle, String prefer) {
    Bundle responseBundle = new Bundle();
    bundle.getEntry().forEach(entry -> {
      try {
        responseBundle.addEntry(perform(entry, prefer));
      } catch (Exception e) {
        FhirException fhirException = findFhirException(e);
        if (fhirException != null) {
          BundleEntryResponseComponent responseEntry = new BundleEntryResponseComponent();
          responseEntry.setStatus("" + fhirException.getHttpCode());
          BundleEntryComponent responseBundleEntry = responseBundle.addEntry();
          responseBundleEntry.addLink().setRelation(LinkRelationTypes.ALTERNATE).setUrl(entry.getFullUrl());
          OperationOutcome outcome = new OperationOutcome();
          outcome.setIssue(fhirException.getIssues());
          responseBundleEntry.setResource(outcome);
          responseBundleEntry.setResponse(responseEntry);
          return;
        }
        throw new RuntimeException("entry: " + entry.getFullUrl(), e);
      }
    });
    responseBundle.setType(BundleType.BATCHRESPONSE);
    return responseBundle;
  }

  private Bundle transaction(Bundle bundle, String prefer) {
    return tx.transaction(() -> {
      Bundle responseBundle = new Bundle();
      bundle.getEntry().forEach(entry -> {
        try {
          responseBundle.addEntry(perform(entry, prefer));
        } catch (Exception e) {
          FhirException fhirException = findFhirException(e);
          if (fhirException != null) {
            fhirException.addExtension("fullUrl", entry.getFullUrl());
            fhirException.getIssues().forEach(i -> i.addExpression("Bundle.entry[" + bundle.getEntry().indexOf(entry) + "]"));
          }
          throw e;
        }
      });
      responseBundle.setType(BundleType.TRANSACTIONRESPONSE);
      return responseBundle;
    });
  }

  private BundleEntryComponent perform(BundleEntryComponent entry, String prefer) {
    if (entry.getRequest().getMethod() == HTTPVerb.NULL) {
      //XXX hack  @see #validateTransaction
      return new BundleEntryComponent().setResponse(new BundleEntryResponseComponent().setStatus("200"));
    }
    FhirestRequest req = buildRequest(entry);
    req.setOperation(endpointService.findOperation(req));
    FhirestResponse resp = endpointService.execute(req);

    BundleEntryComponent newEntry = new BundleEntryComponent();
    newEntry.setResponse(new BundleEntryResponseComponent().setStatus(resp.getStatus().toString()).setLocation(getLocation(resp)));
    if (resp.getBody() != null && resp.getBody() instanceof Resource) {
      newEntry.setResource((Resource) resp.getBody());
    }
    if (resp.getBody() != null && resp.getBody() instanceof ResourceContent) {
      newEntry.setResource(resourceFormatService.parse((ResourceContent) resp.getBody()));
    }
    if (entry.getFullUrl() != null) {
      newEntry.addLink().setRelation(LinkRelationTypes.ALTERNATE).setUrl(entry.getFullUrl());
    }
    if (StringUtils.equals(prefer, PreferredReturn.OperationOutcome)) {
      newEntry.getResponse().setOutcome(new OperationOutcome());
    }
    return newEntry;
  }

  private FhirestRequest buildRequest(BundleEntryComponent entry) {
    FhirestRequest req = new FhirestRequest();
    String method = entry.getRequest().getMethod().toCode();
    req.setTransactionMethod(method);
    URI uri;
    Extension transactionGeneratedId = entry.getRequest().getExtensionByUrl(BundleReferenceHandler.URN__GENERATED_ID);
    if (method.equals(HTTPVerb.POST.toCode()) && transactionGeneratedId != null) {
      req.setMethod(HTTPVerb.PUT.toCode());
      uri = URI.create(((UriType) transactionGeneratedId.getValue()).getValue());
    } else {
      req.setMethod(method);
      String url = entry.getRequest().getUrl();
      url = url.replaceAll("\\|", URLEncoder.encode("|", StandardCharsets.UTF_8));
      uri = URI.create(url);
    }
    if (StringUtils.isEmpty(uri.getPath())) {
      throw new FhirException(FhirestIssue.FEST_027, "uri", uri);
    }
    req.setType(StringUtils.substringBefore(uri.getPath(), "/"));
    req.setPath(StringUtils.removeStart(uri.getPath(), req.getType()));
    req.putQuery(uri.getQuery());
    req.putHeader("If-None-Exist", entry.getRequest().getIfNoneExist());
    req.setContentType(MediaType.APPLICATION_JSON);
    if (entry.getResource() != null) {
      req.setBody(resourceFormatService.compose(entry.getResource(), "json").getValue());
    }
    return req;
  }

  private String getLocation(FhirestResponse resp) {
    if (resp.getHeader("Content-Location") != null) {
      return resp.getHeader("Content-Location");
    }
    if (resp.getHeader("Location") != null) {
      return resp.getHeader("Location");
    }
    return null;
  }

  private FhirException findFhirException(Throwable e) {
    if (e instanceof FhirException) {
      return (FhirException) e;
    }
    if (e.getCause() != null) {
      return findFhirException(e.getCause());
    }
    return null;
  }

}
