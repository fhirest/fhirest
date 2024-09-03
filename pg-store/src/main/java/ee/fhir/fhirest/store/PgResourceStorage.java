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
    if (clientIdentity.get() != null && clientIdentity.get().getName() != null) {
      version.setAuthor(Map.of("name", clientIdentity.get().getName()));
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
  public List<ResourceVersion> load(List<VersionId> ids) {
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
