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

package ee.fhir.fhirest.core.service.stu;

import ee.fhir.fhirest.core.model.InteractionType;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.core.service.resource.ResourceService;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.stereotype.Component;

/**
 * XXX
 * some beatuful day remove structures dependency from fhirest-core and move this to structures module
 * also move FhirPath.
 * and make fhir-structures dependant on fhir-core
 *
 * ponies
 */
@Component
@RequiredArgsConstructor
public class StuResourceService {
  private final ResourceService resourceService;
  private final ResourceFormatService formatService;

  public <T extends Resource> T getResource(VersionId id) {
    return formatService.parse(resourceService.load(id).getContent().getValue());
  }

  public <T extends Resource> T getResource(String reference) {
    return formatService.parse(resourceService.load(reference).getContent().getValue());
  }

  public void save(Resource r) {
    save(new ResourceId(r.getResourceType().name(), r.getId()), r);
  }

  public void save(ResourceId id, Resource r) {
    String mime = "json";
    ResourceContent json = formatService.compose(r, mime);
    String interaction = id.getResourceId() == null ? InteractionType.CREATE : InteractionType.UPDATE;
    resourceService.save(id, json, interaction);
  }

}
