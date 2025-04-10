/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
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

package ee.fhir.fhirest.core.model.search;

import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.model.VersionId;
import ee.fhir.fhirest.structure.api.ResourceContent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

@Getter
public class SearchResult {
  private Integer total;
  private List<ResourceVersion> entries;
  private final List<ResourceVersion> includes = new ArrayList<>();
  private ResourceContent issues;

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
    return new SearchResult(0, Collections.emptyList());
  }

  public boolean isEmpty() {
    return CollectionUtils.isEmpty(entries);
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public void setEntries(List<ResourceVersion> entries) {
    this.entries = entries;
  }

  public void addInclude(ResourceVersion include) {
    this.includes.add(include);
  }

  public void addIncludes(List<ResourceVersion> includes) {
    this.includes.addAll(includes);
  }

  public void setIssues(ResourceContent issues) {
    this.issues = issues;
  }

}
