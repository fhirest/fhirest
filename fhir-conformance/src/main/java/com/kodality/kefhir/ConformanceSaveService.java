package com.kodality.kefhir;

import com.kodality.kefhir.core.api.resource.ResourceAfterSaveInterceptor;
import com.kodality.kefhir.core.api.resource.ResourceStorage;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import java.util.Date;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class ConformanceSaveService {
  private final ResourceStorage storehouse;
  private final List<ResourceAfterSaveInterceptor> afterSaveInterceptors;
  private final ResourceFormatService formatService;

  public void save(Resource r, Date modified) {
    ResourceId resourceId = new ResourceId(r.getResourceType().name(), r.getId());
    ResourceVersion current = storehouse.load(new VersionId(resourceId));
    if (current == null || (modified != null && current.getModified().before(modified))) {
      ResourceVersion version = storehouse.save(resourceId, formatService.compose(r, "json"));
      afterSaveInterceptors.stream().filter(i -> i.getPhase().equals(ResourceAfterSaveInterceptor.TRANSACTION)).forEach(i -> i.handle(version));
    }
  }

}
