/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.SearchCriterion;
import ee.fhir.fhirest.core.model.search.SearchResult;
import ee.fhir.fhirest.core.service.resource.ResourceSearchService;
import ee.fhir.fhirest.core.service.resource.ResourceService;
import ee.fhir.fhirest.core.util.ResourceUtil;
import ee.fhir.fhirest.structure.util.ResourcePropertyUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r5.model.Bundle.HTTPVerb;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.UriType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BundleReferenceHandler {
  public static final String URN__GENERATED_ID = "urn:fhirest-transaction-generated-id";
  private final ResourceService resourceService;
  private final ResourceSearchService resourceSearchService;

  public void replaceIds(Bundle bundle) {
    // fullUrl -> local key
    Map<String, String> referenceIds = new HashMap<>();
    bundle.getEntry().forEach(entry -> {
      BundleEntryRequestComponent request = entry.getRequest();
      if (request.getMethod() == HTTPVerb.PUT) {
        if (request.getUrl().contains("?")) {
          // XXX this seems so stupid to identifiy conditional updates here and like so.
          // but we need to prepare real id here to replace ids in other resources.
          // XXX also all of this duplicates logic from #FhirResourceServer.conditionalUpdate
          ResourceId foundId = find(request.getUrl());
          if (foundId != null) {
            referenceIds.put(entry.getFullUrl(), foundId.getResourceReference());
            return;
          }
          String ref = generateNewId(entry.getResource().getResourceType().name());
          referenceIds.put(entry.getFullUrl(), ref);
          request.setUrl(ref);
          return;
        }
        VersionId id = ResourceUtil.parseReference(request.getUrl());
        referenceIds.put(entry.getFullUrl(), id.getResourceReference());
        return;
      }
      if (request.getMethod() == HTTPVerb.POST) {
        String ref = generateNewId(entry.getResource().getResourceType().name());
        referenceIds.put(entry.getFullUrl(), ref);
        request.addExtension(URN__GENERATED_ID, new UriType(ref));
      }
    });

    // it is possible not to define 'fullUrl' in request. so we remove those 'null's, because we are too lazy to add 'if's
    referenceIds.remove(null);
    bundle.getEntry().forEach(e -> {
      Resource resource = e.getResource();
      ResourcePropertyUtil.findProperties(resource, Reference.class).forEach(reference -> {
        if (referenceIds.containsKey(reference.getReference())) {
          reference.setReference(referenceIds.get(reference.getReference()));
        }
      });
      ResourcePropertyUtil.findProperties(resource, UriType.class).forEach(uri -> {
        // url, oid, uuid
        if (referenceIds.containsKey(uri.getValue())) {
          uri.setValue(referenceIds.get(uri.getValue()));
        }
      });
    });
  }

  private String generateNewId(String resourceType) {
    return resourceType + "/" + resourceService.generateNewId(resourceType);
  }

  private ResourceId find(String url) {
    // TODO: find some good url parser?
//    Patient?identifier=xxx
    String type = StringUtils.substringBefore(url, "?");
    Map<String, List<String>> params = new HashMap<>();
    Arrays.stream(StringUtils.substringAfter(url, "?").split("&")).forEach(q -> {
      String[] p = q.split("=");
      params.computeIfAbsent(p[0], x -> new ArrayList<>(1)).add(p[1]);
    });
    params.put(SearchCriterion._COUNT, Collections.singletonList("1"));
    SearchResult result = resourceSearchService.search(type, params);
    if (result.getTotal() > 1) {
      throw new FhirException(FhirestIssue.FEST_002, "uri", url, "total", result.getTotal());
    }
    return result.getTotal() == 1 ? result.getEntries().get(0).getId() : null;
  }
}
