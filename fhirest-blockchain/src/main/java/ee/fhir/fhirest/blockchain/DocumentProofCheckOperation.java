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
 package ee.fhir.fhirest.blockchain;

import ee.fhir.fhirest.core.api.resource.InstanceOperationDefinition;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.service.resource.ResourceService;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.ResourceType;
import org.hl7.fhir.r5.model.StringType;

@Component
@RequiredArgsConstructor
public class DocumentProofCheckOperation implements InstanceOperationDefinition {
  private final DocumentNotary notary;
  private final ResourceService resourceService;
  private final ResourceFormatService formatService;

  @Override
  public String getResourceType() {
    return ResourceType.Patient.name();
  }

  @Override
  public String getOperationName() {
    return "proof-check";
  }

  @Override
  public ResourceContent run(ResourceId id, ResourceContent parameters) {
    ResourceVersion version = resourceService.load(id.getReference());
    String documentUnchanged = notary.checkDocument(version);
    Parameters parameter = new Parameters();
    parameter.addParameter().setName("valid").setValue(new StringType().setValue(documentUnchanged));
    return formatService.compose(parameter, "json");
  }
}
