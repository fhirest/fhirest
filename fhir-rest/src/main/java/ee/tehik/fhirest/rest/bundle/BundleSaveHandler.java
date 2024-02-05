package ee.tehik.fhirest.rest.bundle;

import io.micronaut.context.annotation.DefaultImplementation;
import org.hl7.fhir.r5.model.Bundle;

@DefaultImplementation(BundleService.class)
public interface BundleSaveHandler {
  Bundle save(Bundle bundle, String prefer);
}
