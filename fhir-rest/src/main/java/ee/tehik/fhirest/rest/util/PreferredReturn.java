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
package ee.tehik.fhirest.rest.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public final class PreferredReturn {
  private static final String type = "return";
  public static final String minimal = "minimal";
  public static final String representation = "representation";
  public static final String OperationOutcome = "OperationOutcome";

  private PreferredReturn() {
    //
  }

  public static String parse(Map<String, List<String>> headers) {
    if (headers == null || !headers.containsKey("Prefer") || headers.get("Prefer").isEmpty()) {
      return null;
    }
    return parse(headers.get("Prefer").get(0));
  }

  public static String parse(String preferredHeader) {
    if (preferredHeader == null) {
      return null;
    }
    return Stream.of(StringUtils.split(preferredHeader, ";"))
        .map(p -> StringUtils.split(p, "="))
        .filter(p -> p[0].equals(type))
        .findAny()
        .map(p -> p[1])
        .orElse(null);
  }

}
