package ee.fhir.fhirest.rest.bundle;

import org.hl7.fhir.r5.model.Bundle;

public interface BundleSaveHandler {
  Bundle save(Bundle bundle, String prefer);
}
