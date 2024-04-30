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

import ee.fhir.fhirest.core.api.resource.ResourceAfterDeleteInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceBeforeSaveInterceptor;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.HistorySearchCriterion;
import ee.fhir.fhirest.core.util.ResourceUtil;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.tx.TransactionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceService {
  private final ResourceStorageService storageService;
  private final List<ResourceBeforeSaveInterceptor> beforeSaveInterceptors;
  private final List<ResourceAfterSaveInterceptor> afterSaveInterceptors;
  private final List<ResourceAfterDeleteInterceptor> afterDeleteInterceptor;
  private final TransactionService tx;

  public ResourceVersion save(ResourceId id, ResourceContent content, String interaction) {
    interceptBeforeSave(ResourceBeforeSaveInterceptor.INPUT_VALIDATION, id, content, interaction);
    interceptBeforeSave(ResourceBeforeSaveInterceptor.NORMALIZATION, id, content, interaction);
    interceptBeforeSave(ResourceBeforeSaveInterceptor.BUSINESS_VALIDATION, id, content, interaction);

    id.setResourceId(id.getResourceId() == null ? generateNewId(id.getResourceType()) : id.getResourceId());
    ResourceVersion version = tx.transaction(() -> {
      interceptBeforeSave(ResourceBeforeSaveInterceptor.TRANSACTION, id, content, interaction);
      ResourceVersion ver = storageService.store(id, content);
      interceptAfterSave(ResourceAfterSaveInterceptor.TRANSACTION, ver);
      return ver;
    });
    interceptAfterSave(ResourceAfterSaveInterceptor.FINALIZATION, version);
    return version;
  }

  /**
   * @param reference ResourceType/id
   */
  public ResourceVersion load(String reference) {
    return load(ResourceUtil.parseReference(reference));
  }

  public ResourceVersion load(ResourceId id) {
    return load(new VersionId(id));
  }

  public ResourceVersion load(VersionId id) {
    return storageService.load(id);
  }

  public List<ResourceVersion> load(List<VersionId> ids) {
    return storageService.load(ids);
  }

  public List<ResourceVersion> loadHistory(HistorySearchCriterion criteria) {
    return storageService.loadHistory(criteria);
  }

  public void delete(ResourceId id) {
    storageService.delete(id);
    afterDeleteInterceptor.forEach(i -> i.delete(id));
  }

  public String generateNewId(String resourceType) {
    return storageService.generateNewId(resourceType);
  }

  private void interceptBeforeSave(String phase, ResourceId id, ResourceContent content, String interaction) {
    beforeSaveInterceptors.stream().filter(i -> i.getPhase().equals(phase)).forEach(i -> i.handle(id, content, interaction));
  }

  private void interceptAfterSave(String phase, ResourceVersion version) {
    afterSaveInterceptors.stream().filter(i -> i.getPhase().equals(phase)).forEach(i -> i.handle(version));
  }

}
