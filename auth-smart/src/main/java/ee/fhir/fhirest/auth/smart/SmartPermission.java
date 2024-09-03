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

package ee.fhir.fhirest.auth.smart;

import ee.fhir.fhirest.core.model.InteractionType;
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
