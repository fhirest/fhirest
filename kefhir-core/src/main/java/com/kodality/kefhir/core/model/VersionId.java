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
 package com.kodality.kefhir.core.model;

public class VersionId extends ResourceId {
  private Integer version;
  private String fullUrl;

  public VersionId(String resourceType) {
    super(resourceType);
  }

  public VersionId(String resourceType, String resourceId) {
    super(resourceType, resourceId);
  }

  public VersionId(ResourceId id) {
    this(id.getResourceType(), id.getResourceId());
  }

  public VersionId(String resourceType, String resourceId, Integer version) {
    this(resourceType, resourceId);
    this.version = version;
  }

  @Override
  public String getReference() {
    if (version == null) {
      return super.getReference();
    }
    return super.getReference() + "/_history/" + version;
  }

  public String getETag() {
    if (version == null) {
      return "";
    }
    return "W/\"" + version + "\"";
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getFullUrl() {
    return fullUrl;
  }

  public void setFullUrl(String fullUrl) {
    this.fullUrl = fullUrl;
  }
}
