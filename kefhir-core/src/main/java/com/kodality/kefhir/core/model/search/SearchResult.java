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
package com.kodality.kefhir.core.model.search;

import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class SearchResult {
  private Integer total;
  private List<ResourceVersion> entries;
  private final List<ResourceVersion> includes = new ArrayList<>();

  public SearchResult() {
    this(0, new ArrayList<>());
  }

  public SearchResult(Integer total, List<ResourceVersion> entries) {
    this.total = total;
    this.entries = entries;
  }

  public static SearchResult lazy(Integer total, List<ResourceId> ids) {
    return new SearchResult(total, ids.stream().map(id -> new ResourceVersion(new VersionId(id), null)).collect(Collectors.toList()));
  }

  public static SearchResult empty() {
    return new SearchResult(0, Collections.<ResourceVersion>emptyList());
  }

  public boolean isEmpty() {
    return CollectionUtils.isEmpty(entries);
  }

  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public List<ResourceVersion> getEntries() {
    return entries;
  }

  public void setEntries(List<ResourceVersion> entries) {
    this.entries = entries;
  }

  public List<ResourceVersion> getIncludes() {
    return includes;
  }

  public void addInclude(ResourceVersion include) {
    this.includes.add(include);
  }

  public void addIncludes(List<ResourceVersion> includes) {
    this.includes.addAll(includes);
  }

}
