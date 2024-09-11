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

package ee.fhir.fhirest.core.service.resource;

import ee.fhir.fhirest.core.api.resource.ResourceAfterDeleteInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceBeforeSaveInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceStorage;
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

/**
 * <p>Service responsible for resource save/update/delete interactions.</p>
 * <p>Manages resource lifecycle, transaction, interceptors.</p>
 *
 * @see ResourceStorageService
 * @see ResourceBeforeSaveInterceptor
 * @see ResourceAfterSaveInterceptor
 * @see ResourceAfterDeleteInterceptor
 * @see TransactionService
 */
@Component
@RequiredArgsConstructor
public class ResourceService {
  private final ResourceStorageService storageService;
  private final List<ResourceBeforeSaveInterceptor> beforeSaveInterceptors;
  private final List<ResourceAfterSaveInterceptor> afterSaveInterceptors;
  private final List<ResourceAfterDeleteInterceptor> afterDeleteInterceptor;
  private final TransactionService tx;

  /**
   * <p>Call interceptors, start transaction and save resource using implemented {@link ResourceStorage}</p>
   * <br/>
   * <p>
   * Interceptors order:
   *   <ul>
   *     <li>ResourceBeforeSaveInterceptor.INPUT_VALIDATION</li>
   *     <li>ResourceBeforeSaveInterceptor.NORMALIZATION</li>
   *     <li>ResourceBeforeSaveInterceptor.BUSINESS_VALIDATION</li>
   *     <li><i>- transaction start -</i></li>
   *     <li>ResourceBeforeSaveInterceptor.TRANSACTION</li>
   *     <li><i>- perform save -</i></li>
   *     <li>ResourceAfterSaveInterceptor.TRANSACTION</li>
   *     <li><i>- transaction end -</i></li>
   *     <li>ResourceAfterSaveInterceptor.FINALIZATION</li>
   *   </ul>
   * </p>
   *
   * @param id          FHIR Resource id
   * @param content     FHIR resource content
   * @param interaction FHIR interaction
   * @return Resource version with content and id
   */
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
   * @param reference "{ResourceType}/{id}[/_history/{versionId}]", eg. "Patient/1" or "Patient/1/_history/2"
   * @see ResourceService#load(VersionId)
   */
  public ResourceVersion load(String reference) {
    return load(ResourceUtil.parseReference(reference));
  }

  /**
   * Load last version by resource id
   *
   * @see ResourceService#load(VersionId)
   */
  public ResourceVersion load(ResourceId id) {
    return load(new VersionId(id));
  }

  /**
   * Load resource version using implemented {@link ee.fhir.fhirest.core.service.resource.ResourceStorageService ResourceStorageService}.
   * If version id is not provided - loads last version.
   *
   * @return Resource version with content and id
   */
  public ResourceVersion load(VersionId id) {
    return storageService.load(id);
  }

  /**
   * Load resource versions using implemented {@link ee.fhir.fhirest.core.service.resource.ResourceStorageService ResourceStorageService}.
   *
   * @return Resource versions with content and id
   */
  public List<ResourceVersion> load(List<VersionId> ids) {
    return storageService.load(ids);
  }

  /**
   * Perform resource history search if supported.
   *
   * @return List of all found resource versions.
   * @see <a href="https://www.hl7.org/fhir/http.html#history">https://www.hl7.org/fhir/http.html#history</a>
   */
  public List<ResourceVersion> loadHistory(HistorySearchCriterion criteria) {
    return storageService.loadHistory(criteria);
  }

  /**
   * Perform <b>delete</b> interaction and call all {@link ResourceAfterDeleteInterceptor}
   *
   * @see <a href="https://www.hl7.org/fhir/http.html#delete">https://www.hl7.org/fhir/http.html#delete</a>
   */
  public void delete(ResourceId id) {
    storageService.delete(id);
    afterDeleteInterceptor.forEach(i -> i.delete(id));
  }

  /**
   * Prepare a new unique resource id to be saved in the future.
   */
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
