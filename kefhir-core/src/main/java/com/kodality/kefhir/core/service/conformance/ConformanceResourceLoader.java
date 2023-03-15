package com.kodality.kefhir.core.service.conformance;

import io.micronaut.context.annotation.DefaultImplementation;
import java.util.List;
import org.hl7.fhir.r5.model.Resource;


@DefaultImplementation(DefaultConformanceResourceLoader.class)
public interface ConformanceResourceLoader {

  <T extends Resource> List<T> load(String name);
}
