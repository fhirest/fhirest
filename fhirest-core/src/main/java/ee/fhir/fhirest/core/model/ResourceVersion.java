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
 package ee.fhir.fhirest.core.model;

import ee.fhir.fhirest.structure.api.ResourceContent;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceVersion implements Serializable {
  private VersionId id;
  private ResourceContent content;

  private Map<String, Object> author;
  private Date modified;
  private boolean deleted;

  public ResourceVersion() {
    //
  }

  public ResourceVersion(VersionId id, ResourceContent content) {
    this.id = id;
    this.content = content;
  }

  public String getReference() {
    return getId().getReference();
  }

  public String getETag() {
    return getId().getETag();
  }

}
