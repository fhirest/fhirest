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

import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.core.api.resource.InstanceOperationDefinition;
import com.kodality.kefhir.core.api.resource.TypeOperationDefinition;
import com.kodality.kefhir.core.model.InteractionType;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.ResourceVersionPolicy;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.CapabilityStatement.SystemRestfulInteraction;
import org.hl7.fhir.r5.model.StructureDefinition;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class KefhirEndpointInitializer implements ConformanceUpdateListener {
  private final KefhirEndpointService endpointService;
  private final List<FhirResourceServer> resourceServers;
  private final DefaultFhirResourceServer defaultResourceServer;
  private final FhirRootServer rootServer;

  private final List<InstanceOperationDefinition> instanceOperations;
  private final List<TypeOperationDefinition> typeOperations;

  private CapabilityStatement capability;

  @Override
  public void updated() {
    capability = prepareCapability(ConformanceHolder.getCapabilityStatement(), ConformanceHolder.getDefinitions());
    restart();
  }

  public CapabilityStatement getModifiedCapability() {
    return capability;
  }

  private void restart() {
    stop();
    if (capability != null) {
      capability.getRest().stream().filter(r -> r.getMode() == RestfulCapabilityMode.SERVER).forEach(r -> start(r));
    }
  }

  private void stop() {
    endpointService.getEnabledOperations().clear();
  }

  private CapabilityStatement prepareCapability(CapabilityStatement capability, List<StructureDefinition> definitions) {
    if (capability == null || CollectionUtils.isEmpty(definitions)) {
      return null;
    }
    List<String> defined = definitions.stream().map(d -> d.getName()).collect(toList());

    // multiple capabilities. how should we handle these?
    CapabilityStatement capabilityStatement = capability.copy();
    capabilityStatement.setText(null);
    capabilityStatement.getRest().forEach(rest -> {
      rest.setResource(rest.getResource().stream().filter(rr -> defined.contains(rr.getType())).collect(toList()));
      rest.getResource().forEach(rr -> {
        rr.setVersioning(ResourceVersionPolicy.VERSIONED);// shouldn't this be in fhirs 'full' conformance?
      });
    });
    capabilityStatement.getRest().forEach(rest -> {
      prepareOperations(rest);
      List<String> interactions = asList("transaction", "batch", SystemRestfulInteraction.HISTORYSYSTEM.toCode());
      rest.setInteraction(rest.getInteraction()
          .stream()
          .filter(i -> interactions.contains(i.getCode().toCode()))
          .collect(toList()));
      rest.getResource().forEach(rr -> rr.setReferencePolicy(Collections.emptyList()));
    });

    return capabilityStatement;
  }

  /**
   * currenty rewrites capability operations with system operations.
   * this doesn't see to be very correct
   * propaply should either leave as is, either filter capability operations with system operations.
   * and this url thingy looks like a hack
   */
  private void prepareOperations(CapabilityStatementRestComponent rest) {
    rest.getResource().forEach( r-> {
      r.setOperation(new ArrayList<>());
    instanceOperations.stream().filter(io -> io.getResourceType().equals(r.getType()))
        .forEach(io -> r.getOperation().add(new CapabilityStatementRestResourceOperationComponent().setName(io.getOperationName())
        .setDefinition("http://hl7.org/fhir/OperationDefinition/" + io.getResourceType() + "-" + io.getOperationName())));
    typeOperations.stream().filter(io -> io.getResourceType().equals(r.getType()))
        .forEach(to -> r.getOperation().add(new CapabilityStatementRestResourceOperationComponent().setName(to.getOperationName())
        .setDefinition("http://hl7.org/fhir/OperationDefinition/" + to.getResourceType() + "-" + to.getOperationName())));
    });
  }

  private void start(CapabilityStatementRestComponent rest) {
    endpointService.startRoot(rootServer);
    rest.getResource().forEach(this::start);
    log.info("Started " + (rest.getResource().size() + 1) + " rest services.");
  }

  private void start(CapabilityStatementRestResourceComponent resourceRest) {
    String type = resourceRest.getType();
    List<String> interactions = resourceRest.getInteraction().stream().filter(i -> i.getCode() != null).map(i -> i.getCode().toCode()).collect(toList());
    if (CollectionUtils.isNotEmpty(resourceRest.getOperation())) {
      interactions.add(InteractionType.OPERATION); // XXX for some reason operation is not in default fhir capability statement. need to add it.
    }
    log.debug("Starting: " + type + ": " + String.join(", ", interactions));
    FhirResourceServer service = resourceServers.stream().filter(s -> s.getTargetType().equals(type)).findFirst().orElse(defaultResourceServer);
    interactions.forEach(i -> endpointService.start(type, i, service));
  }
}
