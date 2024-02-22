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
package ee.fhir.fhirest.core.service.resource;

import ee.fhir.fhirest.core.api.resource.ResourceStorage;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.HistorySearchCriterion;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.tx.TransactionService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.springframework.stereotype.Component;

@Component
public class ResourceStorageService {
  private final Map<String, ResourceStorage> storages;
  private final TransactionService tx;

  public ResourceStorageService(List<ResourceStorage> storages, TransactionService tx) {
    this.storages = storages.stream().collect(Collectors.toMap(s -> s.getResourceType(), s -> s));
    this.tx = tx;
  }

  private ResourceStorage getStorage(String resourceType) {
    return storages.getOrDefault(resourceType, storages.get(ResourceStorage.DEFAULT));
  }

  public ResourceVersion load(VersionId id) {
    ResourceVersion version = getStorage(id.getResourceType()).load(id);
    if (version == null) {
      throw new FhirException(404, IssueType.NOTFOUND, id.getReference() + " not found");
    }
    return version;
  }

  public List<ResourceVersion> load(List<ResourceId> ids) {
    Map<String, List<ResourceId>> typeIds = ids.stream().collect(Collectors.groupingBy(i -> i.getResourceType()));
    return typeIds.keySet().stream().flatMap(type -> getStorage(type).load(typeIds.get(type)).stream()).collect(Collectors.toList());
  }

  public List<ResourceVersion> loadHistory(HistorySearchCriterion criteria) {
    return getStorage(criteria.getResourceType()).loadHistory(criteria);
  }

  public void delete(ResourceId id) {
    getStorage(id.getResourceType()).delete(id);
  }

  public ResourceVersion store(ResourceId id, ResourceContent content) {
    return getStorage(id.getResourceType()).save(id, content);
  }

  /**
   * use with caution. only business logic
   * outside of transaction
   */
  public ResourceVersion storeForce(ResourceId id, ResourceContent content) {
    return tx.newTransaction(() -> getStorage(id.getResourceType()).save(id, content));
  }

  public String generateNewId(String resourceType) {
    return getStorage(resourceType).generateNewId();
  }

}
