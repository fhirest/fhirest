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
 package com.kodality.kefhir.core.service.resource;

import com.kodality.kefhir.core.api.resource.InstanceOperationDefinition;
import com.kodality.kefhir.core.api.resource.OperationInterceptor;
import com.kodality.kefhir.core.api.resource.TypeOperationDefinition;
import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.structure.api.ResourceContent;
import java.util.List;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

@Singleton
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
