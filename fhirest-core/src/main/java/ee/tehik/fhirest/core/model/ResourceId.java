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
 package ee.tehik.fhirest.core.model;

import java.util.Objects;

public class ResourceId {
  private String resourceId;
  private String resourceType;

  public ResourceId(String resourceType) {
    Objects.requireNonNull(resourceType);
    this.resourceType = resourceType;
  }

  public ResourceId(String resourceType, String resourceId) {
    this(resourceType);
    this.resourceId = resourceId;
  }

  public String getResourceReference() {
    return resourceType + "/" + resourceId;
  }

  public String getReference() {
    return getResourceReference();
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

}
