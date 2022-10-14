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
package com.kodality.kefhir.auth.smart;

import java.util.List;
import java.util.Map;

import static com.kodality.kefhir.core.model.InteractionType.CREATE;
import static com.kodality.kefhir.core.model.InteractionType.DELETE;
import static com.kodality.kefhir.core.model.InteractionType.HISTORYINSTANCE;
import static com.kodality.kefhir.core.model.InteractionType.HISTORYSYSTEM;
import static com.kodality.kefhir.core.model.InteractionType.HISTORYTYPE;
import static com.kodality.kefhir.core.model.InteractionType.READ;
import static com.kodality.kefhir.core.model.InteractionType.SEARCHSYSTEM;
import static com.kodality.kefhir.core.model.InteractionType.SEARCHTYPE;
import static com.kodality.kefhir.core.model.InteractionType.UPDATE;
import static com.kodality.kefhir.core.model.InteractionType.VREAD;

public final class SmartPermission {
  public static final String create = "c";
  public static final String read = "r";
  public static final String update = "u";
  public static final String delete = "d";
  public static final String search = "s";

  //VALIDATE, OPERATION
  public static final Map<String, List<String>> interactions = Map.of(
      create, List.of(CREATE),
      read, List.of(READ, VREAD, HISTORYINSTANCE),
      update, List.of(UPDATE),
      delete, List.of(DELETE),
      search, List.of(SEARCHTYPE, SEARCHSYSTEM, HISTORYTYPE, HISTORYSYSTEM)
  );
}
