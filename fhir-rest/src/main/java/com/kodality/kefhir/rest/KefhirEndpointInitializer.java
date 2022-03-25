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
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.ResourceVersionPolicy;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.SystemRestfulInteraction;
import org.hl7.fhir.r4.model.StructureDefinition;

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

  // XXX: unimplemented stuff
  private CapabilityStatement prepareCapability(CapabilityStatement capability, List<StructureDefinition> definitions) {
    if (capability == null || CollectionUtils.isEmpty(definitions)) {
      return null;
    }
    List<String> defined = definitions.stream().map(d -> d.getName()).collect(toList());
    defined.removeAll(asList("Bundle", "ImplementationGuide", "MessageDefinition", "CompartmentDefinition", "StructureMap", "GraphDefinition", "DataElement"));

    // multiple capabilities. how should be handle these?
    CapabilityStatement capabilityStatement = capability.copy();
    capabilityStatement.setText(null);
    capabilityStatement.getRest().forEach(rest -> {
      rest.setResource(rest.getResource().stream().filter(rr -> defined.contains(rr.getType())).collect(toList()));
      rest.getResource().forEach(rr -> {
        rr.setVersioning(ResourceVersionPolicy.VERSIONED);// shouldn't this be in fhirs 'full' conformance?
      });
    });
    capabilityStatement.getRest().forEach(rest -> {
      rest.setOperation(null);
      List<String> interactions =
          asList("transaction", "batch", SystemRestfulInteraction.HISTORYSYSTEM.toCode());
      rest.setInteraction(rest.getInteraction()
          .stream()
          .filter(i -> interactions.contains(i.getCode().toCode()))
          .collect(toList()));
      rest.getResource().forEach(rr -> {
        rr.setReferencePolicy(Collections.emptyList());
      });
    });

    return capabilityStatement;
  }

  private void start(CapabilityStatementRestComponent rest) {
    endpointService.startRoot(rootServer);
    rest.getResource().forEach(this::start);
  }

  private void start(CapabilityStatementRestResourceComponent resourceRest) {
    String type = resourceRest.getType();
    List<String> interactions = resourceRest.getInteraction().stream().filter(i -> i.getCode() != null).map(i -> i.getCode().toCode()).collect(toList());
    log.info("Starting: " + type + ": " + String.join(", ", interactions));
    FhirResourceServer service = resourceServers.stream().filter(s -> s.getTargetType().equals(type)).findFirst().orElse(defaultResourceServer);
    interactions.forEach(i -> endpointService.start(type, i, service));
  }
}
