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

package ee.fhir.fhirest.rest.util;

import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.SearchResult;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r5.model.Bundle.BundleEntrySearchComponent;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.Bundle.HTTPVerb;
import org.hl7.fhir.r5.model.Bundle.SearchEntryMode;
import java.util.List;

public class BundleUtil {

  public static Bundle compose(List<ResourceVersion> versions, BundleType bundleType) {
    return compose(null, versions, bundleType);
  }

  public static Bundle compose(SearchResult search) {
    Bundle bundle = compose(search.getTotal(), search.getEntries(), BundleType.SEARCHSET);
    if (search.getIssues() != null) {
      bundle.setIssues(ResourceFormatService.get().parse(search.getIssues().getValue()));
    }
    if (CollectionUtils.isNotEmpty(search.getIncludes())) {
      bundle.getEntry().forEach(e -> {
        e.setSearch(new BundleEntrySearchComponent());
        e.getSearch().setMode(SearchEntryMode.MATCH);
      });
      search.getIncludes().forEach(v -> {
        BundleEntryComponent e = composeEntry(v);
        e.setSearch(new BundleEntrySearchComponent());
        e.getSearch().setMode(SearchEntryMode.INCLUDE);
        bundle.addEntry(e);
      });
    }
    return bundle;
  }

  public static Bundle compose(Integer total, List<ResourceVersion> versions, BundleType bundleType) {
    Bundle bundle = new Bundle();
    bundle.setTotal(total == null ? versions.size() : total);
    bundle.setType(bundleType);
    versions.forEach(v -> {
      BundleEntryComponent entry = composeEntry(v);
      bundle.addEntry(entry);
      if (bundleType == BundleType.HISTORY) {
        HTTPVerb httpVerb = calcMethod(v);
        BundleEntryRequestComponent request = new BundleEntryRequestComponent();
        request.setMethod(httpVerb);
        request.setUrl(v.getId().getResourceReference());
        entry.setRequest(request);

        BundleEntryResponseComponent response = new BundleEntryResponseComponent();
        response.setLastModified(v.getModified());
        response.setStatus(calcStatusCode(httpVerb));
        entry.setResponse(response);
      }
    });
    return bundle;
  }

  private static BundleEntryComponent composeEntry(ResourceVersion version) {
    BundleEntryComponent entry = new BundleEntryComponent();
    VersionId id = version.getId();
    entry.setFullUrl(id.getFullUrl() != null ? id.getFullUrl() : id.getResourceReference());
    entry.setResource(ResourceFormatService.get().parse(version.getContent().getValue()));
    return entry;
  }

  private static HTTPVerb calcMethod(ResourceVersion version) {
    //XXX: this is NOT how it should be. need to somehow save action maybe? stupid
    return version.isDeleted() ? HTTPVerb.DELETE : version.getId().getVersion() == 1 ? HTTPVerb.POST : HTTPVerb.PUT;
  }

  private static String calcStatusCode(HTTPVerb httpVerb) {
      return switch (httpVerb) {
          case DELETE -> "204";
          case POST -> "201";
          default -> "200";
      };
  }
}
