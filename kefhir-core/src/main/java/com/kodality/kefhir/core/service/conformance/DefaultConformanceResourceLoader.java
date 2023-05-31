package com.kodality.kefhir.core.service.conformance;

import com.kodality.kefhir.core.service.resource.ResourceSearchService;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.Resource;

import static java.util.stream.Collectors.toList;

@Singleton
@RequiredArgsConstructor
public class DefaultConformanceResourceLoader implements ConformanceResourceLoader {

  private final ResourceSearchService resourceSearchService;
  private final ResourceFormatService resourceFormatService;

  @Override
  public <T extends Resource> List<T> load(String name) {
    return resourceSearchService.search(name, "_count", "9999").getEntries().stream().map(v -> resourceFormatService.<T>parse(v.getContent()))
        .collect(toList());
  }
}
