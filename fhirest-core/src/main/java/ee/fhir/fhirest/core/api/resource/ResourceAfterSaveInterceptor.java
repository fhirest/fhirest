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
 package ee.fhir.fhirest.core.api.resource;

import ee.fhir.fhirest.core.model.ResourceVersion;

public abstract class ResourceAfterSaveInterceptor {
  public static final String TRANSACTION = "TRANSACTION";
  public static final String FINALIZATION = "FINALIZATION";

  private final String phase;

  public ResourceAfterSaveInterceptor(String phase) {
    this.phase = phase;
  }

  public String getPhase() {
    return phase;
  }

  public abstract void handle(ResourceVersion version);
}
