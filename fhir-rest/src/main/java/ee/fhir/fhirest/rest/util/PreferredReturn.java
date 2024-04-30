/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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

package ee.fhir.fhirest.rest.util;

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
