package com.kodality.kefhir.core.api.conformance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;

public interface HapiValidationSupportProvider {
  IValidationSupport getValidationSupport(FhirContext context);
}
