/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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
