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
package ee.tehik.fhirest.search;

import ee.tehik.fhirest.core.api.resource.ResourceAfterDeleteInterceptor;
import ee.tehik.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.tehik.fhirest.core.model.ResourceId;
import ee.tehik.fhirest.core.model.ResourceVersion;
import ee.tehik.fhirest.search.index.IndexService;
import org.springframework.stereotype.Component;

@Component
public class PgSearchResourceSaver extends ResourceAfterSaveInterceptor implements ResourceAfterDeleteInterceptor {
  private IndexService indexService;

  public PgSearchResourceSaver(IndexService indexService) {
    super(ResourceAfterSaveInterceptor.TRANSACTION);
    this.indexService = indexService;
  }

  @Override
  public void handle(ResourceVersion version) {
    indexService.saveIndexes(version);
  }

  @Override
  public void delete(ResourceId id) {
    indexService.deleteResource(id);
  }

}
