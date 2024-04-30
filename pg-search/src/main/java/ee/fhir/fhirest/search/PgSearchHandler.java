/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
