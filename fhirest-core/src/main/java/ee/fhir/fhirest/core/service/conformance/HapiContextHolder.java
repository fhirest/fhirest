/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ee.fhir.fhirest.core.service.conformance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ee.fhir.fhirest.core.api.conformance.ConformanceUpdateListener;
import ee.fhir.fhirest.core.api.conformance.HapiValidationSupportProvider;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r5.model.Enumerations.CodeSystemContentMode;
import org.hl7.fhir.r5.model.PackageInformation;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration and holder class of Hapi context
 */
@RequiredArgsConstructor
@Component
public class HapiContextHolder implements ConformanceUpdateListener {

  public static final List<CodeSystemContentMode> SUPPORTED_CS_CONTENT_MODES = Arrays.asList(CodeSystemContentMode.COMPLETE, CodeSystemContentMode.FRAGMENT);

  protected IWorkerContext hapiContext;
  protected FhirContext context;
  protected FhirValidator validator;
  protected final List<HapiValidationSupportProvider> validationSupportProviders;

  @Value("${fhirest.hapi.cache.timeout:10m}")
  private Duration cacheTimeout;

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
    context = FhirContext.forR5();
  }

  @Override
  public Integer getOrder() {
    return 30;
  }

  @Override
  public void updated() {
    IValidationSupport validationSupport = getValidationSupport();

    hapiContext = new HapiWorkerContext(context, validationSupport);
    context.setValidationSupport(validationSupport);

    validator = context.newValidator();
    validator.registerValidatorModule(new FhirInstanceValidator(validationSupport));
    preloadHapi();
  }

  protected IValidationSupport getValidationSupport() {
    Map<String, IBaseResource> defs = ConformanceHolder.getDefinitions().stream().collect(Collectors.toMap(d -> d.getUrl(), d -> d, (a, b) -> a));
    // hack to bypass hapi structuredefinition.type validation, (#see org.hl7.fhir.validation.instance.InstanceValidator.checkTypeValue)
    // because wo don't have information about "source package" and it isn't even a part of fhir resource.
    defs.values().forEach(def -> ((Resource) def).setSourcePackage(
        new PackageInformation("hl7.fhir.r", context.getVersion().getVersion().getFhirVersionString(), def.getMeta().getLastUpdated())));
    Map<String, IBaseResource> vs = ConformanceHolder.getValueSets().stream().collect(Collectors.toMap(d -> d.getUrl(), d -> d, (a, b) -> a));
    Map<String, IBaseResource> cs = ConformanceHolder.getCodeSystems().stream()
        .filter(c -> SUPPORTED_CS_CONTENT_MODES.contains(c.getContent()))
        .collect(Collectors.toMap(d -> d.getUrl(), d -> d, (a, b) -> a));

    ValidationSupportChain chain = new ValidationSupportChain(
        ValidationSupportChain.CacheConfiguration.defaultValues().setCacheTimeout(cacheTimeout),
        new InMemoryTerminologyServerValidationSupport(context),
        new PrePopulatedValidationSupport(context, defs, vs, cs),
        new CommonCodeSystemsTerminologyService(context),
        new SnapshotGeneratingValidationSupport(context)
    );
    validationSupportProviders.forEach(p -> chain.addValidationSupport(p.getValidationSupport(context)));
//    return new CachingValidationSupport(chain);
    return chain;
  }

  private void preloadHapi() {
    try {
      validator.validateWithResult("{}");
    } catch (Exception e) {
      //ignore
    }
  }
}
