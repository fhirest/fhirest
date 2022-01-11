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
 package com.kodality.kefhir.auth;

import java.util.Map;
import java.util.Set;

public class User {
  private Map<String, Object> claims;
  private Set<String> scopes;

  public Map<String, Object> getClaims() {
    return claims;
  }

  public void setClaims(Map<String, Object> claims) {
    this.claims = claims;
  }

  public Set<String> getScopes() {
    return scopes;
  }

  public void setScopes(Set<String> scopes) {
    this.scopes = scopes;
  }

}
