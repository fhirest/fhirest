package ee.fhir.fhirest.core.service.conformance.loader;

import ee.fhir.fhirest.core.service.resource.ResourceSearchService;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class ConformanceStorageLoader implements ConformanceLoader {
  private final ResourceSearchService resourceSearchService;
  private final ResourceFormatService resourceFormatService;

  @Override
  public <T extends Resource> List<T> load(String name) {
    return resourceSearchService.search(name, "_count", "9999").getEntries().stream().map(v -> resourceFormatService.<T>parse(v.getContent()))
        .collect(toList());
  }
}
