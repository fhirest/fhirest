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

package ee.fhir.fhirest.search.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

public final class SearchPathUtil {

  public static Set<String> parsePaths(String expression) {
    return Stream.of(expression.split("\\|"))
        .map(s -> StringUtils.trim(s))
        .map(s -> replaceAs(s))
        .map(s -> replaceFhirpathAs(s))
        .map(s -> replaceFhirpathOfType(s))
        .map(s -> replaceFhirpathWhereResolve(s))
        .collect(Collectors.toSet());
  }

  private static String replaceAs(String s) {
    if (!s.startsWith("(") || !s.contains(" as ")) {
      return s;
    }
    s = StringUtils.remove(s, "(");
    s = StringUtils.remove(s, ")");
    String[] p = s.split(" as ");
    return p[0] + StringUtils.capitalize(p[1]);
  }

  /**
   * who are we to implement fhirpath?
   * "value.as(Quantity)" seems to mean wh should search by "valueQuantity". hack it!
   */
  private static String replaceFhirpathAs(String s) {
    if (!s.contains(".as(")) {
      return s;
    }
    String[] p = s.split(".as");
    p[1] = StringUtils.remove(p[1], ")");
    p[1] = StringUtils.remove(p[1], "(");
    return p[0] + StringUtils.capitalize(p[1]);
  }

  private static String replaceFhirpathOfType(String s) {
    if (!s.contains(".ofType(")) {
      return s;
    }
    String[] p = s.split(".ofType");
    p[1] = StringUtils.remove(p[1], ")");
    p[1] = StringUtils.remove(p[1], "(");
    return p[0] + StringUtils.capitalize(p[1]);
  }

  /**
   * who are we to implement fhirpath?
   * "Encounter.subject.where(resolve() is Patient)" basically means we should search by "Encounter.subject", but reference query value should have "Patient" in it.
   */
  private static String replaceFhirpathWhereResolve(String s) {
    if (!s.contains(".where(resolve() is ")) {
      return s;
    }
    return RegExUtils.removeAll(s, "\\.where\\(resolve\\(\\) is [a-zA-Z]*\\)");
  }

  public static String getFhirpathWhereResolveType(String s) {
    if (!s.contains(".where(resolve() is ")) {
      return null;
    }
    Matcher m = Pattern.compile(".*\\.where\\(resolve\\(\\) is ([a-zA-Z]*)\\)").matcher(s);
    m.matches();
    return m.group(1);
  }
}
