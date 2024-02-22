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
 package ee.fhir.fhirest.core.model;

public final class InteractionType {
  // http://hl7.org/fhir/restful-interaction
  public static final String READ = "read";
  public static final String VREAD = "vread";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";
  public static final String HISTORYINSTANCE = "history-instance";
  public static final String HISTORYTYPE = "history-type";
  public static final String HISTORYSYSTEM = "history-system";
  public static final String CREATE = "create";
  public static final String SEARCHTYPE = "search-type";
  public static final String SEARCHSYSTEM = "search-system";
  public static final String VALIDATE = "validate";
  public static final String CONFORMANCE = "conformance";
  public static final String TRANSACTION = "transaction";
  public static final String OPERATION = "operation";
}
