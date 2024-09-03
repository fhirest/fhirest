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

package ee.fhir.fhirest.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapUtil {
  private MapUtil() {
    //
  }

  @SuppressWarnings("unchecked")
  public static <V> Map<String, V> toMap(Object... keyValues) {
    Map<String, V> map = new HashMap<>();
    if (keyValues.length == 0) {
      return map;
    }
    for (int i = 0; i < keyValues.length; i = i + 2) {
      map.put((String) keyValues[i], (V) keyValues[i + 1]);
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  public static <V> Map<String, List<V>> toMultimap(Object... keyValues) {
    Map<String, List<V>> map = new HashMap<>();
    if (keyValues.length == 0) {
      return map;
    }
    for (int i = 0; i < keyValues.length; i = i + 2) {
      String key = (String) keyValues[i];
      map.computeIfAbsent(key, k -> new ArrayList<>()).add((V) keyValues[i + 1]);
    }
    return map;
  }

}
