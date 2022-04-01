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
package com.kodality.kefhir.store;

import com.kodality.kefhir.auth.ClientIdentity;
import com.kodality.kefhir.core.api.resource.ResourceStorage;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import com.kodality.kefhir.core.model.search.HistorySearchCriterion;
import com.kodality.kefhir.core.util.DateUtil;
import com.kodality.kefhir.core.util.JsonUtil;
import com.kodality.kefhir.store.repository.ResourceRepository;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import io.micronaut.context.annotation.Primary;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.hl7.fhir.r4.model.Resource;

@Primary
@Singleton
public class PgResourceStorage implements ResourceStorage {
  @Inject
  private ClientIdentity clientIdentity;
  @Inject
  private ResourceFormatService resourceFormatService;
  private final ResourceRepository resourceRepository;

  public PgResourceStorage(ResourceRepository resourceRepository) {
    this.resourceRepository = resourceRepository;
  }

  @Override
  public ResourceVersion save(ResourceId id, ResourceContent content) {
    return store(id, content);
  }

  @Override
  public ResourceVersion saveForce(ResourceId id, ResourceContent content) {
    return store(id, content);
  }

  private ResourceVersion store(ResourceId id, ResourceContent content) {
    ResourceContent cont = content.getContentType().contains("json") ? content : toJson(content);
    ResourceVersion version = new ResourceVersion(new VersionId(id), cont);
    version.getId().setVersion(resourceRepository.getLastVersion(id) + 1);
    if (clientIdentity.get() != null) {
      version.setAuthor(clientIdentity.get().getClaims());
    }
    resourceRepository.create(version);
    return load(version.getId());
  }

  private ResourceContent toJson(ResourceContent content) {
    Resource resource = resourceFormatService.parse(content.getValue());
    return resourceFormatService.compose(resource, "json");
  }

  @Override
  public String generateNewId() {
    return resourceRepository.getNextResourceId();
  }

  @Override
  public void delete(ResourceId id) {
    ResourceVersion current = resourceRepository.load(new VersionId(id));
    if (current == null || current.isDeleted()) {
      return;
    }
    ResourceVersion version = new ResourceVersion();
    version.setId(new VersionId(id));
    version.setDeleted(true);
    version.getId().setVersion(resourceRepository.getLastVersion(id) + 1);
    if (clientIdentity.get() != null) {
      version.setAuthor(clientIdentity.get().getClaims());
    }
    resourceRepository.create(version);
  }

  @Override
  public ResourceVersion load(VersionId id) {
    ResourceVersion version = resourceRepository.load(id);
    decorate(version);
    return version;
  }

  @Override
  public List<ResourceVersion> load(List<ResourceId> ids) {
    List<ResourceVersion> versions = resourceRepository.load(ids);
    versions.forEach(v -> decorate(v));
    return versions;
  }

  @Override
  public List<ResourceVersion> loadHistory(HistorySearchCriterion criteria) {
    List<ResourceVersion> history = resourceRepository.loadHistory(criteria);
    history.forEach(this::decorate);
    return history;
  }

  @SuppressWarnings("unchecked")
  private void decorate(ResourceVersion version) {
    // TODO: maybe rewrite this when better times come and resource will be parsed until end.
    if (version == null || version.getContent().getValue() == null) {
      return;
    }

    Map<String, Object> resource =
        version.getContent().getValue() != null ? JsonUtil.fromJson(version.getContent().getValue()) : new HashMap<>();
    resource.put("id", version.getId().getResourceId());
    resource.put("resourceType", version.getId().getResourceType());
    Map<Object, Object> meta = (Map<Object, Object>) resource.getOrDefault("meta", new HashMap<>());
    meta.put("versionId", "" + version.getId().getVersion());
    meta.put("lastUpdated", new SimpleDateFormat(DateUtil.FHIR_DATETIME).format(version.getModified()));
    resource.put("meta", meta);

    version.getContent().setValue(JsonUtil.toJson(resource));
  }

}
