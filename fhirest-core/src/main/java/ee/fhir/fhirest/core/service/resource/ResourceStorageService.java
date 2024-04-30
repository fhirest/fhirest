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

package ee.fhir.fhirest.core.service.resource;

import ee.fhir.fhirest.core.api.resource.ResourceStorage;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.HistorySearchCriterion;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.tx.TransactionService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ResourceStorageService {
  private final Map<String, ResourceStorage> storages;
  private final TransactionService tx;

  public ResourceStorageService(List<ResourceStorage> storages, TransactionService tx) {
    this.storages = storages.stream().collect(Collectors.toMap(ResourceStorage::getResourceType, s -> s));
    this.tx = tx;
  }

  private ResourceStorage getStorage(String resourceType) {
    return storages.getOrDefault(resourceType, storages.get(ResourceStorage.DEFAULT));
  }

  public ResourceVersion load(VersionId id) {
    ResourceVersion version = getStorage(id.getResourceType()).load(id);
    if (version == null) {
      throw new FhirException(FhirestIssue.FEST_028, "ref", id.getReference());
    }
    return version;
  }

  public List<ResourceVersion> load(List<VersionId> ids) {
    Map<String, List<VersionId>> typeIds = ids.stream().collect(Collectors.groupingBy(ResourceId::getResourceType));
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
