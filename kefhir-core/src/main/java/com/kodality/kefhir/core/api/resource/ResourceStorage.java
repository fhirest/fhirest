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
package com.kodality.kefhir.core.api.resource;

import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import com.kodality.kefhir.core.model.search.HistorySearchCriterion;
import com.kodality.kefhir.structure.api.ResourceContent;
import java.util.List;

public interface ResourceStorage {
  String DEFAULT = "default";

  default String getResourceType() {
    return DEFAULT;
  }

  /**
   * transaction supports
   */
  ResourceVersion save(ResourceId id, ResourceContent content);

  /**
   * transaction requires new
   */
  ResourceVersion saveForce(ResourceId id, ResourceContent content);

  void delete(ResourceId id);

  ResourceVersion load(VersionId id);

  List<ResourceVersion> load(List<ResourceId> ids);

  List<ResourceVersion> loadHistory(HistorySearchCriterion criteria);

  String generateNewId();
}