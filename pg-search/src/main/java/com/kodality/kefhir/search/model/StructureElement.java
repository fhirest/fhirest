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

public class StructureElement {
  private final String base;
  private final String path;
  private final String type;
  private boolean isMany;

  public StructureElement(String base, String path, String type) {
    this.base = base;
    this.path = path;
    this.type = type;
  }

  public String getBase() {
    return base;
  }

  public String getPath() {
    return path;
  }

  public String getType() {
    return type;
  }

  public boolean isMany() {
    return isMany;
  }

  public void setMany(boolean isMany) {
    this.isMany = isMany;
  }

}
