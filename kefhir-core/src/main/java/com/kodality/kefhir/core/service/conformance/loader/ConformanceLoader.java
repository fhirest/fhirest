package com.kodality.kefhir.core.service.conformance.loader;

import io.micronaut.context.annotation.DefaultImplementation;
import java.util.List;
import org.hl7.fhir.r5.model.Resource;


@DefaultImplementation(ConformanceStorageLoader.class)
public interface ConformanceLoader {

  <T extends Resource> List<T> load(String name);
}
