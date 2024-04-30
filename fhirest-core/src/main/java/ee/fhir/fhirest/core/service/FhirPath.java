/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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

package ee.fhir.fhirest.core.service;

import ee.fhir.fhirest.core.api.conformance.ConformanceUpdateListener;
import ee.fhir.fhirest.core.service.conformance.HapiContextHolder;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.springframework.stereotype.Component;

@Component
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
