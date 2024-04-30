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
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FormatFilter implements FhirestRequestFilter {
  private static final String FORMAT = "_format";
  private final ResourceFormatService resourceFormatService;

  @Override
  public Integer getOrder() {
    return READ;
  }

  @Override
  public void handleRequest(FhirestRequest req) {
    String format = req.getParameter(FORMAT);
    if (format != null) {
      String mime = resourceFormatService.findPresenter(format).map(p -> p.getMimeTypes().get(0))
          .orElseThrow(() -> new FhirException(FhirestIssue.FEST_005));
      req.setAccept(List.of(MediaType.valueOf(mime)));
      req.setContentType(MediaType.valueOf(mime));
      req.getParameters().remove(FORMAT);
    }
    if (req.getAccept() == null) {
      req.setAccept(MediaType.ALL);
    }
    if (req.getAccept().size() == 1 && req.getAccept().get(0).toString().equals(MediaType.ALL.toString()) && req.getContentType() != null) {
      req.setAccept(List.of(req.getContentType(), MediaType.ALL));
    }
    if (resourceFormatService.findSupported(req.getAccept().stream().map(mt -> mt.getType() + "/" + mt.getSubtype()).toList()).isEmpty()) {
      throw new FhirException(FhirestIssue.FEST_003);
    }
  }
}
