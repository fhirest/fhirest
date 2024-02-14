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
package ee.tehik.fhirest.rest;

import ee.tehik.fhirest.core.exception.FhirServerException;
import ee.tehik.fhirest.core.model.InteractionType;
import ee.tehik.fhirest.core.model.ResourceVersion;
import ee.tehik.fhirest.core.model.search.HistorySearchCriterion;
import ee.tehik.fhirest.core.service.conformance.ConformanceHolder;
import ee.tehik.fhirest.core.service.resource.ResourceService;
import ee.tehik.fhirest.rest.bundle.BundleSaveHandler;
import ee.tehik.fhirest.rest.interaction.FhirInteraction;
import ee.tehik.fhirest.rest.model.FhirestRequest;
import ee.tehik.fhirest.rest.model.FhirestResponse;
import ee.tehik.fhirest.rest.util.BundleUtil;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import jakarta.inject.Provider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FhirRootServer {
  private final Provider<FhirestEndpointInitializer> restResourceInitializer;
  private final ResourceFormatService resourceFormatService;
  private final ResourceService resourceService;
  private final Provider<BundleSaveHandler> bundleService;

  @FhirInteraction(interaction = InteractionType.CONFORMANCE, mapping = "GET /metadata")
  public FhirestResponse conformance(FhirestRequest req) {
    String mode = req.getParameter("mode");
    if ("terminology".equals(mode)) {
      return new FhirestResponse(200, ConformanceHolder.getTerminologyCapabilities());
    }
    return new FhirestResponse(200, restResourceInitializer.get().getModifiedCapability());
  }

  @FhirInteraction(interaction = InteractionType.TRANSACTION, mapping = "POST /")
  public FhirestResponse transaction(FhirestRequest req) {
    if (StringUtils.isEmpty(req.getBody())) {
      return new FhirestResponse(204);
    }
    String prefer = req.getHeader("Prefer");
    Bundle responseBundle = bundleService.get().save(resourceFormatService.parse(req.getBody()), prefer);
    return new FhirestResponse(200, responseBundle);
  }

  @FhirInteraction(interaction = InteractionType.HISTORYSYSTEM, mapping = "GET /_history")
  public FhirestResponse history(FhirestRequest req) {
    HistorySearchCriterion criteria = new HistorySearchCriterion();
    criteria.setSince(req.getParameter(HistorySearchCriterion._SINCE));
    criteria.setCount(req.getParameter(HistorySearchCriterion._COUNT));
    List<ResourceVersion> versions = resourceService.loadHistory(criteria);
    return new FhirestResponse(200, BundleUtil.compose(null, versions, BundleType.HISTORY));
  }

  @FhirInteraction(interaction = InteractionType.SEARCHSYSTEM, mapping = "GET /_search")
  public FhirestResponse search(FhirestRequest req) {
    throw new FhirServerException(501, "system search not implemented");
  }

  @FhirInteraction(interaction = "custom", mapping = "GET /")
  public FhirestResponse welcome(FhirestRequest req) {
    OperationOutcome op = new OperationOutcome();
    op.addIssue().setDetails(new CodeableConcept().setText("Welcome to Fhirest"));
    return new FhirestResponse(200, op);
  }

}
