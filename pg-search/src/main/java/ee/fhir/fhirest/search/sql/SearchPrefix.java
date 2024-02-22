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
 package ee.fhir.fhirest.search.sql;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

public class SearchPrefix {
  public static final String eq = "eq";
  public static final String ne = "ne";
  public static final String gt = "gt";
  public static final String lt = "lt";
  public static final String ge = "ge";
  public static final String le = "le";
  public static final String sa = "sa";
  public static final String eb = "eb";
  public static final String ap = "ap";

  private final String prefix;
  private final String value;

  public SearchPrefix(String prefix, String value) {
    this.prefix = prefix;
    this.value = value;
  }

  public static SearchPrefix parse(String value, Collection<String> prefixes) {
    return parse(value, prefixes.toArray(new String[] {}));
  }

  public static SearchPrefix parse(String value, String... prefixes) {
    if (value == null) {
      return new SearchPrefix(null, null);
    }
    if (!StringUtils.startsWithAny(value, prefixes)) {
      return new SearchPrefix(null, value);
    }
    return new SearchPrefix(value.substring(0, 2), value.substring(2));
  }

  public String getPrefix() {
    return prefix;
  }

  public String getValue() {
    return value;
  }

}
