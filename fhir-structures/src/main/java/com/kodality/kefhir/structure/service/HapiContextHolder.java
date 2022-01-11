package com.kodality.kefhir.structure.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;

@Singleton
public class HapiContextHolder {
  private IWorkerContext hapiContext;
  private FhirContext context;

  public IWorkerContext getHapiContext() {
    return hapiContext;
  }

  public FhirContext getContext() {
    return context;
  }

  @PostConstruct
  private void init() {
    context = FhirContext.forR4();
    hapiContext = new HapiWorkerContext(context, new DefaultProfileValidationSupport(context));
  }
}
