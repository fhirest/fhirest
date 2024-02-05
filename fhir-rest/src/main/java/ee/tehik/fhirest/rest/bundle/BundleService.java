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
package ee.tehik.fhirest.rest.bundle;

import ee.tehik.fhirest.core.exception.FhirException;
import ee.tehik.fhirest.core.model.search.SearchCriterion;
import ee.tehik.fhirest.core.model.search.SearchCriterionBuilder;
import ee.tehik.fhirest.core.model.search.SearchResult;
import ee.tehik.fhirest.core.service.resource.ResourceSearchService;
import ee.tehik.fhirest.rest.FhirestEndpointService;
import ee.tehik.fhirest.rest.ServerUriHelper;
import ee.tehik.fhirest.rest.model.FhirestRequest;
import ee.tehik.fhirest.rest.model.FhirestResponse;
import ee.tehik.fhirest.rest.util.PreferredReturn;
import ee.tehik.fhirest.structure.api.ResourceContent;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import ee.tehik.fhirest.tx.TransactionService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.context.ServerRequestContext;
import jakarta.inject.Singleton;
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
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.UriType;

@Singleton
@RequiredArgsConstructor
public class BundleService implements BundleSaveHandler {
  private final ResourceSearchService searchService;
  private final BundleReferenceHandler bundleReferenceHandler;
  private final FhirestEndpointService endpointService;
  private final ResourceFormatService resourceFormatService;
  private final TransactionService tx;
  private final ServerUriHelper serverUriHelper;

  @Override
  public Bundle save(Bundle bundle, String prefer) {
    if (bundle.getEntry().stream().anyMatch(e -> !e.hasRequest())) {
      throw new FhirException(400, IssueType.INVALID, "Bundle.request element required");
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
    throw new FhirException(400, IssueType.INVALID, "only batch or transaction supported");
  }

  private void validateTransaction(Bundle bundle) {
    bundle.getEntry().forEach(entry -> {
      if (entry.getRequest().getMethod() == HTTPVerb.POST && entry.getRequest().getIfNoneExist() != null) {
        String ifNoneExist = entry.getRequest().getIfNoneExist() + "&_count=0";
        String type = entry.getResource().getResourceType().name();
        SearchCriterion criteria = SearchCriterionBuilder.parse(ifNoneExist, type);
        SearchResult result = searchService.search(criteria);
        if (result.getTotal() == 1) {
          entry.getRequest().setMethod(HTTPVerb.NULL); //ignore
        }
        if (result.getTotal() > 1) {
          String msg = "was expecting 0 or 1 resources. found " + result.getTotal();
          throw new FhirException(412, IssueType.PROCESSING, msg);
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
          responseEntry.setStatus("" + fhirException.getStatusCode());
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
      bundle.getEntry().stream().forEach(entry -> {
        try {
          responseBundle.addEntry(perform(entry, prefer));
        } catch (Exception e) {
          FhirException fhirException = findFhirException(e);
          if (fhirException != null) {
            fhirException.addExtension("fullUrl", entry.getFullUrl());
            fhirException.getIssues().forEach(i -> {
              String expr = "Bundle.entry[" + bundle.getEntry().indexOf(entry) + "]";
              i.addExpression(expr);
            });
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
    if (StringUtils.isEmpty(req.getType())) {
      throw new FhirException(400, IssueType.PROCESSING, "what are you trying to do, mister?");
    }
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
    HttpRequest<Object> request = ServerRequestContext.currentRequest().orElseThrow();
    req.setServerUri(serverUriHelper.buildServerUri(request));
    req.setServerHost(serverUriHelper.getHost(request));
    String method = entry.getRequest().getMethod().toCode();
    req.setTransactionMethod(method);
    URI uri;
    Extension transactionGeneratedId = entry.getRequest().getExtensionByUrl("urn:fhirest-transaction-generated-id");
    if (method.equals("POST") && transactionGeneratedId != null) {
      req.setMethod("PUT");
      uri = URI.create(((UriType) transactionGeneratedId.getValue()).getValue());
    } else {
      req.setMethod(method);
      String url = entry.getRequest().getUrl();
      url = url.replaceAll("\\|", URLEncoder.encode("|", StandardCharsets.UTF_8));
      uri = URI.create(url);
    }
    req.setType(StringUtils.substringBefore(uri.getPath(), "/"));
    req.setPath(StringUtils.removeStart(uri.getPath(), req.getType()));
    req.setUri(uri.toString());
    req.putQuery(uri.getQuery());
    req.putHeader("If-None-Exist", entry.getRequest().getIfNoneExist());
    req.setContentType(MediaType.APPLICATION_JSON_TYPE);
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
