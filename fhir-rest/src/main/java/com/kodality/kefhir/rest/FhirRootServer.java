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
package com.kodality.kefhir.rest;

import com.kodality.kefhir.core.exception.FhirServerException;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.search.HistorySearchCriterion;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.core.service.resource.ResourceService;
import com.kodality.kefhir.rest.bundle.BundleSaveHandler;
import com.kodality.kefhir.rest.interaction.FhirInteraction;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;
import com.kodality.kefhir.rest.util.BundleUtil;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import jakarta.inject.Provider;
import java.util.List;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.OperationOutcome;

import static com.kodality.kefhir.core.model.InteractionType.CONFORMANCE;
import static com.kodality.kefhir.core.model.InteractionType.HISTORYSYSTEM;
import static com.kodality.kefhir.core.model.InteractionType.SEARCHSYSTEM;
import static com.kodality.kefhir.core.model.InteractionType.TRANSACTION;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class FhirRootServer {
  private final Provider<KefhirEndpointInitializer> restResourceInitializer;
  private final ResourceFormatService resourceFormatService;
  private final ResourceService resourceService;
  private final Provider<BundleSaveHandler> bundleService;

  @FhirInteraction(interaction = CONFORMANCE, mapping = "GET /metadata")
  public KefhirResponse conformance(KefhirRequest req) {
    String mode = req.getParameter("mode");
    if ("terminology".equals(mode)) {
      return new KefhirResponse(200, ConformanceHolder.getTerminologyCapabilities());
    }
    return new KefhirResponse(200, restResourceInitializer.get().getModifiedCapability());
  }

  @FhirInteraction(interaction = TRANSACTION, mapping = "POST /")
  public KefhirResponse transaction(KefhirRequest req) {
    if (StringUtils.isEmpty(req.getBody())) {
      return new KefhirResponse(204);
    }
    String prefer = req.getHeader("Prefer");
    Bundle responseBundle = bundleService.get().save(resourceFormatService.parse(req.getBody()), prefer);
    return new KefhirResponse(200, responseBundle);
  }

  @FhirInteraction(interaction = HISTORYSYSTEM, mapping = "GET /_history")
  public KefhirResponse history(KefhirRequest req) {
    HistorySearchCriterion criteria = new HistorySearchCriterion();
    criteria.setSince(req.getParameter(HistorySearchCriterion._SINCE));
    criteria.setCount(req.getParameter(HistorySearchCriterion._COUNT));
    List<ResourceVersion> versions = resourceService.loadHistory(criteria);
    return new KefhirResponse(200, BundleUtil.compose(null, versions, BundleType.HISTORY));
  }

  @FhirInteraction(interaction = SEARCHSYSTEM, mapping = "GET /_search")
  public KefhirResponse search(KefhirRequest req) {
    throw new FhirServerException(501, "system search not implemented");
  }

  @FhirInteraction(interaction = "custom", mapping = "GET /")
  public KefhirResponse welcome(KefhirRequest req) {
    OperationOutcome op = new OperationOutcome();
    op.addIssue().setDetails(new CodeableConcept().setText("Welcome to Kefhir"));
    return new KefhirResponse(200, op);
  }

}
