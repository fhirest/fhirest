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
package com.kodality.kefhir.auth.smart;

import com.kodality.kefhir.auth.ClientIdentity;
import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.InteractionType;
import com.kodality.kefhir.rest.filter.KefhirRequestExecutionInterceptor;
import com.kodality.kefhir.rest.model.KefhirRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

import static com.kodality.kefhir.core.model.InteractionType.CREATE;
import static com.kodality.kefhir.core.model.InteractionType.DELETE;
import static com.kodality.kefhir.core.model.InteractionType.HISTORYINSTANCE;
import static com.kodality.kefhir.core.model.InteractionType.HISTORYTYPE;
import static com.kodality.kefhir.core.model.InteractionType.OPERATION;
import static com.kodality.kefhir.core.model.InteractionType.SEARCHTYPE;
import static com.kodality.kefhir.core.model.InteractionType.UPDATE;
import static com.kodality.kefhir.core.model.InteractionType.VREAD;
import static java.util.Arrays.asList;

@RequiredArgsConstructor
@Singleton
public class SmartAuthHttpInterceptor implements KefhirRequestExecutionInterceptor {
  private final ClientIdentity clientIdentity;
  private final Map<String, Map<String, List<String>>> contexts = new HashMap<>();
  {
    HashMap<String, List<String>> patient = new HashMap<>();
    patient.put(Right.read, asList(InteractionType.READ, VREAD, HISTORYINSTANCE, SEARCHTYPE));
    patient.put(Right.write, asList(UPDATE, DELETE, OPERATION));
    patient.put(Right.all, asList(InteractionType.READ, VREAD, HISTORYINSTANCE, SEARCHTYPE, UPDATE, DELETE, OPERATION));
    contexts.put(Context.patient, patient);

    HashMap<String, List<String>> user = new HashMap<>();
    user.put(Right.read, asList(InteractionType.READ, VREAD, HISTORYINSTANCE, SEARCHTYPE, HISTORYTYPE));
    user.put(Right.write, asList(UPDATE, DELETE, CREATE, InteractionType.VALIDATE, OPERATION));
    user.put(Right.all,
        asList(InteractionType.READ, VREAD, HISTORYINSTANCE, SEARCHTYPE, HISTORYTYPE, UPDATE, DELETE, CREATE, InteractionType.VALIDATE, OPERATION));
    contexts.put(Context.user, user);
  }

  @Override
  public void beforeExecute(KefhirRequest request) {
    validate(request.getOperation().getInteraction(), request.getType());
  }

  public void validate(String interaction, String resourceType) {
    if (!clientIdentity.isAuthenticated()) {
      return; //TODO think
    }

    Set<String> clientScopes = clientIdentity.get().getScopes();
    if (clientScopes == null || clientScopes.stream()
        .map(s -> new SmartScope(s))
        .noneMatch(s -> isScopeAllowed(s, resourceType, interaction))) {
      throw new FhirException(403, IssueType.FORBIDDEN, resourceType + "." + interaction + " not allowed");
    }
  }

  private boolean isScopeAllowed(SmartScope scope, String resourceType, String interaction) {
    boolean resourceTypeAllowed = scope.getResourceType().equals("*") || scope.getResourceType().equals(resourceType);
    if (!resourceTypeAllowed) {
      return false;
    }
    if (!contexts.containsKey(scope.getContext())) {
      throw new FhirException(403, IssueType.FORBIDDEN, "unknown context");
    }
    Map<String, List<String>> contextPermissions = contexts.get(scope.getContext());
    if (!contextPermissions.containsKey(scope.getRights())) {
      throw new FhirException(403, IssueType.FORBIDDEN, "unknown right");
    }

    return contextPermissions.get(scope.getRights()).contains(interaction);
  }

}
