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
import ee.tehik.fhirest.structure.api.ResourceContent;

public abstract class ResourceBeforeSaveInterceptor {
  public static final String INPUT_VALIDATION = "INPUT_VALIDATION"; //structure validations. make sure input is structurally correct
  public static final String NORMALIZATION = "NORMALIZATION";//modify resource if required

  public static final String BUSINESS_VALIDATION = "BUSINESS_VALIDATION";
  public static final String TRANSACTION = "TRANSACTION";

  private final String phase;

  public ResourceBeforeSaveInterceptor(String phase) {
    this.phase = phase;
  }

  public String getPhase() {
    return phase;
  }

  public abstract void handle(ResourceId id, ResourceContent content, String interaction);
}
