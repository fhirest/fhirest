package ee.fhir.fhirest.core.service.conformance.loader;

import java.util.List;
import org.hl7.fhir.r5.model.Resource;


public interface ConformanceLoader {
  <T extends Resource> List<T> load(String name);
}
