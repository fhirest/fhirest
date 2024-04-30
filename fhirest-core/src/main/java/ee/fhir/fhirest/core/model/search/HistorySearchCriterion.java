/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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

import ee.fhir.fhirest.core.util.DateUtil;
import java.util.Date;

public class HistorySearchCriterion {
  public static final String _COUNT = "_count";
  public static final String _SINCE = "_since";

  private String resourceType;
  private String resourceId;
  private Integer count = 100;
  private Date since;

  public HistorySearchCriterion() {
    //
  }

  public HistorySearchCriterion(String resourceType) {
    this.resourceType = resourceType;
  }

  public HistorySearchCriterion(String resourceType, String resourceId) {
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public Date getSince() {
    return since;
  }

  public void setSince(Date since) {
    this.since = since;
  }

  public void setSince(String since) {
    setSince(DateUtil.parse(since));
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count == null ? 100 : count;
  }

  public void setCount(String count) {
    setCount(count == null ? null : Integer.valueOf(count));
  }

}
