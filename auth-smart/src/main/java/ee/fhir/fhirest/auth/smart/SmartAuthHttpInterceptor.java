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
