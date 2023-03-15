/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kodality.kefhir.core.service;

import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.core.service.conformance.HapiContextHolder;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import java.util.List;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

@Singleton
@RequiredArgsConstructor
public class FhirPath implements ConformanceUpdateListener {
  private FHIRPathEngine engine;
  private final ResourceFormatService formatService;
  private final HapiContextHolder hapiContextHolder;

  @Override
  public void updated() {
    engine = new FHIRPathEngine(hapiContextHolder.getHapiContext());
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> evaluate(Resource resource, String expression) {
    try {
      return (List<T>) engine.evaluate(resource, expression);
    } catch (FHIRException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> List<T> evaluate(String content, String expression) {
    return evaluate(formatService.parse(content), expression);
  }

  @Override
  public Integer getOrder() {
    return 200;
  }
}
