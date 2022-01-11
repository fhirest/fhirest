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
 package com.kodality.kefhir.blockchain;

import com.kodality.kefhir.core.api.resource.InstanceOperationDefinition;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.service.resource.ResourceService;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;

@Singleton
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
