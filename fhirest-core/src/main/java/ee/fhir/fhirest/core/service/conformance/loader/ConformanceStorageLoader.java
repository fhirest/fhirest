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

package ee.fhir.fhirest.core.service.conformance.loader;

import ee.fhir.fhirest.core.service.resource.ResourceSearchService;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class ConformanceStorageLoader implements ConformanceLoader {
  private final ResourceSearchService resourceSearchService;
  private final ResourceFormatService resourceFormatService;

  @Override
  public <T extends Resource> List<T> load(String name) {
    return resourceSearchService.search(name, "_count", "9999").getEntries().stream().map(v -> resourceFormatService.<T>parse(v.getContent()))
        .collect(toList());
  }
}
