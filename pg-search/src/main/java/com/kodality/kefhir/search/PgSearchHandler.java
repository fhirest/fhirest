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

import com.kodality.kefhir.core.api.resource.ResourceSearchHandler;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.search.SearchCriterion;
import com.kodality.kefhir.core.model.search.SearchResult;
import com.kodality.kefhir.search.repository.PgSearchRepository;
import java.util.List;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class PgSearchHandler implements ResourceSearchHandler {
  private final PgSearchRepository pgSearchRepository;

  @Override
  public SearchResult search(SearchCriterion criteria) {
    Integer total = pgSearchRepository.count(criteria);
    if (total == 0) {
      return SearchResult.empty();
    }
    List<ResourceId> ids = pgSearchRepository.search(criteria);
    return SearchResult.lazy(total, ids);
  }

}
