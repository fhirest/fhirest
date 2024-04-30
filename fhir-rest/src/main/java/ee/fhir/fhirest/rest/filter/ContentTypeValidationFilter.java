/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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

package ee.fhir.fhirest.rest.filter;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.structure.service.ContentTypeService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ContentTypeValidationFilter implements FhirestRequestFilter {
  private final List<MediaType> supportedMediaTypes;

  public ContentTypeValidationFilter(ContentTypeService contentTypeService) {
    this.supportedMediaTypes = MediaType.parseMediaTypes(contentTypeService.getMediaTypes());
  }

  @Override
  public Integer getOrder() {
    return VALIDATE;
  }

  @Override
  public void handleRequest(FhirestRequest req) {
    if (!req.getAccept().isEmpty() &&
        req.getAccept().stream().noneMatch(a -> a.equalsTypeAndSubtype(MediaType.ALL) || a.isPresentIn(supportedMediaTypes))) {
      throw new FhirException(FhirestIssue.FEST_003);
    }
    if (req.getContentType() != null && !req.getContentType().isPresentIn(supportedMediaTypes)) {
      throw new FhirException(FhirestIssue.FEST_004);
    }
  }

}
