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
package ee.tehik.fhirest.auth.smart;

import ee.tehik.fhirest.core.model.InteractionType;
import java.util.List;
import java.util.Map;

public final class SmartPermission {
  public static final String create = "c";
  public static final String read = "r";
  public static final String update = "u";
  public static final String delete = "d";
  public static final String search = "s";

  //VALIDATE, OPERATION
  public static final Map<String, List<String>> interactions = Map.of(
      create, List.of(InteractionType.CREATE),
      read, List.of(InteractionType.READ, InteractionType.VREAD, InteractionType.HISTORYINSTANCE),
      update, List.of(InteractionType.UPDATE),
      delete, List.of(InteractionType.DELETE),
      search, List.of(InteractionType.SEARCHTYPE, InteractionType.SEARCHSYSTEM, InteractionType.HISTORYTYPE, InteractionType.HISTORYSYSTEM)
  );
}
