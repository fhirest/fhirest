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
package ee.fhir.fhirest.rest.util;

import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.SearchResult;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r5.model.Bundle.BundleEntrySearchComponent;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.Bundle.HTTPVerb;
import org.hl7.fhir.r5.model.Bundle.SearchEntryMode;

public class BundleUtil {

  public static Bundle compose(List<ResourceVersion> versions, BundleType bundleType) {
    return compose(null, versions, bundleType);
  }

  public static Bundle compose(SearchResult search) {
    Bundle bundle = compose(search.getTotal(), search.getEntries(), BundleType.SEARCHSET);
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
        BundleEntryRequestComponent request = new BundleEntryRequestComponent();
        request.setMethod(calcMethod(v));
        request.setUrl(v.getId().getResourceReference());
        entry.setRequest(request);

        BundleEntryResponseComponent response = new BundleEntryResponseComponent();
        response.setLastModified(v.getModified());
        response.setStatus("200");//this is stupid
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
}
