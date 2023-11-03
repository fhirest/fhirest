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
package com.kodality.kefhir.search;

import com.kodality.kefhir.core.api.resource.ResourceAfterDeleteInterceptor;
import com.kodality.kefhir.core.api.resource.ResourceAfterSaveInterceptor;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.search.index.IndexService;
import jakarta.inject.Singleton;

@Singleton
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
