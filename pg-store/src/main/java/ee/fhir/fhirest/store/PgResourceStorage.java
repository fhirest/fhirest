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
package ee.fhir.fhirest.store;

import ee.fhir.fhirest.auth.ClientIdentity;
import ee.fhir.fhirest.core.api.resource.ResourceStorage;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.model.search.HistorySearchCriterion;
import ee.fhir.fhirest.core.util.DateUtil;
import ee.fhir.fhirest.core.util.JsonUtil;
import ee.fhir.fhirest.store.repository.ResourceRepository;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import jakarta.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
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

  private ResourceVersion store(ResourceId id, ResourceContent content) {
    ResourceContent cont = content.getContentType().contains("json") ? content : toJson(content);
    ResourceVersion version = new ResourceVersion(new VersionId(id), cont);
    version.getId().setVersion(resourceRepository.getLastVersion(id) + 1);
    if (clientIdentity.get() != null) {
      version.setAuthor(clientIdentity.get().getClaims());
    }
    resourceRepository.create(version, findProfiles(version));
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
    resourceRepository.create(version, null);
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

  private List<String> findProfiles(ResourceVersion version) {
    Resource resource = resourceFormatService.parse(version.getContent());
    if (resource.getMeta() == null || resource.getMeta().getProfile() == null) {
      return null;
    }
    return resource.getMeta().getProfile().stream()
        .map(p -> p.getValue())
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

}
