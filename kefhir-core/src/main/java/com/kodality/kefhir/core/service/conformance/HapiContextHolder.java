package com.kodality.kefhir.core.service.conformance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;

@RequiredArgsConstructor
@Singleton
public class HapiContextHolder implements ConformanceUpdateListener {
  private IWorkerContext hapiContext;
  private FhirContext context;
  private final ResourceFormatService resourceFormatService;

  public IWorkerContext getHapiContext() {
    return hapiContext;
  }

  public FhirContext getContext() {
    return context;
  }

  @Override
  public void updated() {
    context = FhirContext.forR4();
    hapiContext = new HapiWorkerContext(context, new DefaultProfileValidationSupport(context));
//    try {
//      Map<String, byte[]> map = ConformanceHolder.getDefinitions().stream()
//          .collect(Collectors.toMap(c -> c.getName(), c -> resourceFormatService.compose(c, "json").getBytes()));
//      hapiContext = SimpleWorkerContext.fromDefinitions(map, null);
//    } catch (IOException e) {
//      throw new RuntimeException("hapi hapi hapi...");
//    }
  }

}
