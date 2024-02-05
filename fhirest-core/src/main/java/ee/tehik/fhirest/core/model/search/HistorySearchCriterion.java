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
package ee.tehik.fhirest.core.model.search;

import ee.tehik.fhirest.core.util.DateUtil;
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
