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
package com.kodality.kefhir.structure.service;

import com.kodality.kefhir.structure.api.ResourceRepresentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.inject.Singleton;

@Singleton
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
