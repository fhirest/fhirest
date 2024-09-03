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

package ee.fhir.fhirest.structure.service;

import ee.fhir.fhirest.structure.api.ResourceRepresentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ContentTypeService {
  private final Map<String, String> mimes;
  private final List<String> mediaTypes;

  public ContentTypeService(List<ResourceRepresentation> representations) {
    mimes = new HashMap<>();
    mediaTypes = new ArrayList<>();
    representations.forEach(presenter -> {
      String main = presenter.getMimeTypes().get(0);
      presenter.getMimeTypes().forEach(mime -> {
        if (mimes.containsKey(mime)) {
          throw new IllegalStateException(" multiple composers for mime " + mime);
        }
        mimes.put(mime, main);
        if (mime.contains("/")) {
          mediaTypes.add(mime);
        }
      });
    });
  }

  public List<String> getMediaTypes() {
    return mediaTypes;
  }

  public String getMimeType(String type) {
    return mimes.get(type);
  }

  public boolean isSameType(String t1, String t2) {
    return Objects.equals(t1, t2) || Objects.equals(getMimeType(t1), getMimeType(t2));
  }

}
