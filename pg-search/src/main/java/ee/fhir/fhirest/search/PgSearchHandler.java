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
 package ee.fhir.fhirest.search;

import ee.fhir.fhirest.core.api.resource.ResourceSearchHandler;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.search.SearchCriterion;
import ee.fhir.fhirest.core.model.search.SearchResult;
import ee.fhir.fhirest.search.repository.PgSearchRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
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
