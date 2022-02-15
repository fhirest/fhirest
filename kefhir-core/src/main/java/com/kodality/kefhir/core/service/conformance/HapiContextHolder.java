package com.kodality.kefhir.core.service.conformance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;

@RequiredArgsConstructor
@Singleton
public class HapiContextHolder implements ConformanceUpdateListener {
  private IWorkerContext hapiContext;
  private FhirContext context;
  private FhirValidator validator;

  public IWorkerContext getHapiContext() {
    return hapiContext;
  }

  public FhirContext getContext() {
    return context;
  }

  public FhirValidator getValidator() {
    return validator;
  }

  @PostConstruct
  public void init() {
    context = FhirContext.forR4();
  }

  @Override
  public void updated() {
    Map<String, IBaseResource> defs = ConformanceHolder.getDefinitions().stream().collect(Collectors.toMap(d -> d.getUrl(), d -> d));
    Map<String, IBaseResource> vs = ConformanceHolder.getValueSets().stream().collect(Collectors.toMap(d -> d.getUrl(), d -> d));
    Map<String, IBaseResource> cs = ConformanceHolder.getCodeSystems().stream().collect(Collectors.toMap(d -> d.getUrl(), d -> d));
    PrePopulatedValidationSupport validationSupport = new PrePopulatedValidationSupport(context, defs, vs, cs);

    hapiContext = new HapiWorkerContext(context, validationSupport);

    ValidationSupportChain chain = new ValidationSupportChain(validationSupport);
    FhirInstanceValidator instanceValidator = new FhirInstanceValidator(new CachingValidationSupport(chain));
    validator = context.newValidator();
    validator.registerValidatorModule(instanceValidator);
  }

}