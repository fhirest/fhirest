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
 package com.kodality.kefhir.search.model;

public class Blindex {
  public static final String PIZZELLE = "pizzelle";
  public static final String PARASOL = "parasol";

  private String resourceType;
  private String path;
  private String name;

  public Blindex() {
    //
  }

  public Blindex(String resourceType, String path) {
    this.resourceType = resourceType;
    this.path = path;
  }

  public String getKey() {
    return resourceType + "." + path;
  }

  public String getName() {
    return name;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setName(String name) {
    this.name = name;
  }
}
