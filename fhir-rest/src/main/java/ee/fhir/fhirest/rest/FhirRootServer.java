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
import ee.fhir.fhirest.core.exception.FhirServerException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.InteractionType;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.search.HistorySearchCriterion;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import ee.fhir.fhirest.core.service.resource.ResourceOperationService;
import ee.fhir.fhirest.core.service.resource.ResourceService;
import ee.fhir.fhirest.rest.bundle.BundleSaveHandler;
import ee.fhir.fhirest.rest.interaction.FhirInteraction;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.rest.model.FhirestResponse;
import ee.fhir.fhirest.rest.operation.OperationParametersReader;
import ee.fhir.fhirest.rest.util.BundleUtil;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
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
  private final ResourceOperationService resourceOperationService;
  private final OperationParametersReader operationParametersReader;

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

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "GET /${}")
  public FhirestResponse baseOperation(FhirestRequest req) {
    String operation = req.getPath();
    if (!operation.startsWith("$")) {
      throw new FhirException(FhirestIssue.FEST_010);
    }
    ResourceContent content = operationParametersReader.readOperationParameters(operation, req);
    ResourceContent response = resourceOperationService.runBaseOperation(operation, req.getType(), content);
    return new FhirestResponse(200, response);
  }

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "POST /${}")
  public FhirestResponse baseOperation_(FhirestRequest req) {
    return baseOperation(req);
  }

}
