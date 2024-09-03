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

package ee.fhir.fhirest;

import ee.fhir.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.fhir.fhirest.core.api.resource.ResourceStorage;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.Resource;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConformanceSaveService {
  private final ResourceStorage storehouse;
  private final List<ResourceAfterSaveInterceptor> afterSaveInterceptors;
  private final ResourceFormatService formatService;

  public void save(Resource r, Date modified) {
    ResourceId resourceId = new ResourceId(r.getResourceType().name(), r.getId());
    ResourceVersion current = storehouse.load(new VersionId(resourceId));
    if (current == null || (modified != null && current.getModified().before(modified))) {
      ResourceVersion version = storehouse.save(resourceId, formatService.compose(r, "json"));
      afterSaveInterceptors.stream().filter(i -> i.getPhase().equals(ResourceAfterSaveInterceptor.TRANSACTION)).forEach(i -> i.handle(version));
    }
  }

}
