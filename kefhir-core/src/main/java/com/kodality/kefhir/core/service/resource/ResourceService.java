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

import com.kodality.kefhir.core.api.resource.ResourceAfterDeleteInterceptor;
import com.kodality.kefhir.core.api.resource.ResourceAfterSaveInterceptor;
import com.kodality.kefhir.core.api.resource.ResourceBeforeSaveInterceptor;
import com.kodality.kefhir.core.api.resource.ResourceStorehouse;
import com.kodality.kefhir.core.exception.FhirException;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import com.kodality.kefhir.core.model.search.HistorySearchCriterion;
import com.kodality.kefhir.core.util.ResourceUtil;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.tx.TransactionService;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

import static com.kodality.kefhir.core.api.resource.ResourceAfterSaveInterceptor.FINALIZATION;
import static com.kodality.kefhir.core.api.resource.ResourceBeforeSaveInterceptor.BUSINESS_VALIDATION;
import static com.kodality.kefhir.core.api.resource.ResourceBeforeSaveInterceptor.INPUT_VALIDATION;
import static com.kodality.kefhir.core.api.resource.ResourceBeforeSaveInterceptor.NORMALIZATION;

@Singleton
@RequiredArgsConstructor
public class ResourceService {
  private final ResourceStorehouse storehouse;
  private final List<ResourceBeforeSaveInterceptor> beforeSaveInterceptors;
  private final List<ResourceAfterSaveInterceptor> afterSaveInterceptors;
  private final List<ResourceAfterDeleteInterceptor> afterDeleteInterceptor;
  private final TransactionService tx;

  public ResourceVersion save(ResourceId id, ResourceContent content, String interaction) {
    interceptBeforeSave(INPUT_VALIDATION, id, content, interaction);
    interceptBeforeSave(NORMALIZATION, id, content, interaction);
    interceptBeforeSave(BUSINESS_VALIDATION, id, content, interaction);

    id.setResourceId(id.getResourceId() == null ? generateNewId() : id.getResourceId());
    ResourceVersion version = tx.transaction(() -> {
      interceptBeforeSave(ResourceBeforeSaveInterceptor.TRANSACTION, id, content, interaction);
      ResourceVersion ver = store(id, content);
      interceptAfterSave(ResourceAfterSaveInterceptor.TRANSACTION, ver);
      return ver;
    });
    interceptAfterSave(FINALIZATION, version);
    return version;
  }

  /**
   * @param reference ResourceType/id
   */
  public ResourceVersion load(String reference) {
    return load(ResourceUtil.parseReference(reference));
  }

  public ResourceVersion load(VersionId id) {
    ResourceVersion version = storehouse.load(id);
    if (version == null) {
      throw new FhirException(404, IssueType.NOTFOUND, id.getReference() + " not found");
    }
    return version;
  }

  public List<ResourceVersion> loadHistory(HistorySearchCriterion criteria) {
    return storehouse.loadHistory(criteria);
  }

  public void delete(ResourceId id) {
    storehouse.delete(id);
    afterDeleteInterceptor.forEach(i -> i.delete(id));
  }

  /**
   * use with caution. only business logic
   * inside transaction
   */
  public ResourceVersion store(ResourceId id, ResourceContent content) {
    return storehouse.save(id, content);
  }

  /**
   * use with caution. only business logic
   * outside of transaction
   */
  public ResourceVersion storeForce(ResourceId id, ResourceContent content) {
    return storehouse.saveForce(id, content);
  }

  public String generateNewId() {
    return storehouse.generateNewId();
  }

  private void interceptBeforeSave(String phase, ResourceId id, ResourceContent content, String interaction) {
    beforeSaveInterceptors.stream().filter(i -> i.getPhase().equals(phase)).forEach(i -> i.handle(id, content, interaction));
  }

  private void interceptAfterSave(String phase, ResourceVersion version) {
    afterSaveInterceptors.stream().filter(i -> i.getPhase().equals(phase)).forEach(i -> i.handle(version));
  }

}
