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
package ee.tehik.fhirest.core.api.resource;

import ee.tehik.fhirest.core.model.ResourceId;
import ee.tehik.fhirest.core.model.ResourceVersion;
import ee.tehik.fhirest.core.model.VersionId;
import ee.tehik.fhirest.core.model.search.HistorySearchCriterion;
import ee.tehik.fhirest.structure.api.ResourceContent;
import java.util.List;

public interface ResourceStorage {
  String DEFAULT = "default";

  default String getResourceType() {
    return DEFAULT;
  }

  ResourceVersion save(ResourceId id, ResourceContent content);

  void delete(ResourceId id);

  ResourceVersion load(VersionId id);

  List<ResourceVersion> load(List<ResourceId> ids);

  List<ResourceVersion> loadHistory(HistorySearchCriterion criteria);

  String generateNewId();
}
