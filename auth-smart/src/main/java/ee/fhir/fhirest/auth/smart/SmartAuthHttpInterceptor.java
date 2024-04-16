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
package ee.fhir.fhirest.auth.smart;

import ee.fhir.fhirest.auth.ClientIdentity;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.rest.filter.FhirestRequestExecutionInterceptor;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SmartAuthHttpInterceptor implements FhirestRequestExecutionInterceptor {
  private final ClientIdentity clientIdentity;

  @Override
  public void beforeExecute(FhirestRequest request) {
    validate(request.getOperation().getInteraction(), request.getType());
  }

  public void validate(String interaction, String resourceType) {
    if (!clientIdentity.isAuthenticated()) {
      return; //TODO think
    }

    Set<String> clientScopes = clientIdentity.get().getScopes();
    if (clientScopes == null || clientScopes.stream()
        .map(SmartScope::new)
        .noneMatch(s -> isScopeAllowed(s, resourceType, interaction))) {
      throw new FhirException(403, IssueType.FORBIDDEN, resourceType + "." + interaction + " not allowed");
    }
  }

  private boolean isScopeAllowed(SmartScope scope, String resourceType, String interaction) {
    boolean resourceTypeAllowed = scope.getResourceType().equals("*") || scope.getResourceType().equals(resourceType);
    if (!resourceTypeAllowed) {
      return false;
    }
    if (!SmartContext.contexts.contains(scope.getContext())) {
      throw new FhirException(403, IssueType.FORBIDDEN, "unknown context");
    }
    if (!SmartPermission.interactions.containsKey(scope.getPermissions())) {
      throw new FhirException(403, IssueType.FORBIDDEN, "unknown permission");
    }

    return SmartPermission.interactions.get(scope.getPermissions()).contains(interaction);
  }

}
