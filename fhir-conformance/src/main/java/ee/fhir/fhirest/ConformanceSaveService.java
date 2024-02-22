package ee.fhir.fhirest;

import ee.fhir.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceStorage;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.Resource;

@Slf4j
@Component
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
