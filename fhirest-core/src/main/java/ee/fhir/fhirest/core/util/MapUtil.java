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
