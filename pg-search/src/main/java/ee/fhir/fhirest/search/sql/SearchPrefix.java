/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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
