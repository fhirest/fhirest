package ee.tehik.fhirest;

import ee.tehik.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.tehik.fhirest.core.api.resource.ResourceStorage;
import ee.tehik.fhirest.core.model.ResourceId;
import ee.tehik.fhirest.core.model.ResourceVersion;
import ee.tehik.fhirest.core.model.VersionId;
import ee.tehik.fhirest.structure.service.ResourceFormatService;
import java.util.Date;
import java.util.List;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.Resource;

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
