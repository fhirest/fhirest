/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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

package ee.fhir.fhirest.core.service.resource;

import ee.fhir.fhirest.core.api.resource.InstanceOperationDefinition;
import ee.fhir.fhirest.core.api.resource.OperationInterceptor;
import ee.fhir.fhirest.core.api.resource.TypeOperationDefinition;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.structure.api.ResourceContent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceOperationService {
  private final List<InstanceOperationDefinition> instanceOperations;
  private final List<TypeOperationDefinition> typeOperations;
  private final List<OperationInterceptor> interceptors;

  public ResourceContent runInstanceOperation(String operation, ResourceId id, ResourceContent parameters) {
    interceptors.forEach(i -> i.handle("instance", operation, parameters));
    return instanceOperations.stream()
        .filter(op -> operation.equals("$" + op.getOperationName()))
        .filter(op -> op.getResourceType().equals(id.getResourceType()))
        .findFirst()
        .orElseThrow(() -> new FhirException(404, IssueType.NOTFOUND, "operation " + operation + " not found"))
        .run(id, parameters);
  }

  public ResourceContent runTypeOperation(String operation, String type, ResourceContent parameters) {
    interceptors.forEach(i -> i.handle("type", operation, parameters));
    return typeOperations.stream()
        .filter(op -> operation.equals("$" + op.getOperationName()))
        .filter(op -> op.getResourceType().equals(type))
        .findFirst()
        .orElseThrow(() -> new FhirException(404, IssueType.NOTFOUND, "operation " + operation + " not found"))
        .run(parameters);
  }
}
