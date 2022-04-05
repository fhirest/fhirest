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
package com.kodality.kefhir.structure.util;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class ResourcePropertyUtil {
  private static Map<Class<?>, List<Field>> fieldsCache = new HashMap<>();

  private ResourcePropertyUtil() {
    //
  }

  public static <T> Stream<T> findProperties(Object object, Class<T> fieldClazz) {
    return findProperties(object, new HashSet<>(), fieldClazz).collect(Collectors.toList()).stream();
  }

  @SuppressWarnings("unchecked")
  public static <T> Stream<T> findProperties(Object object, Set<Object> exclude, Class<T> fieldClazz) {
    if (object == null) {
      return Stream.empty();
    }
    if (fieldClazz.equals(object.getClass())) {
      return Stream.of((T) object);
    }
    return getAccessibleFields(object).stream().flatMap(field -> {
      Object obj = getFieldValue(field, object);
      if (obj == null) {
        return Stream.empty();
      }
      if (exclude.contains(obj)) {
        return Stream.empty();
      }
      exclude.add(obj);
      if (fieldClazz.equals(field.getType())) {
        return Stream.of((T) obj);
      }
      if (Type.class.equals(field.getType()) && fieldClazz.equals(obj.getClass())) {
        return Stream.of((T) obj);
      }
      if (obj instanceof Collection) {
        return ((Collection<?>) obj).stream().flatMap(o -> findProperties(o, exclude, fieldClazz));
      }
      if (obj.getClass().getName().startsWith("java")) {
        return Stream.empty();
      }
      return findProperties(obj, exclude, fieldClazz);
    });
  }

  private static List<Field> getAccessibleFields(Object object) {
    return fieldsCache.computeIfAbsent(object.getClass(), c -> {
      return Stream.of(FieldUtils.getAllFields(c)).map(field -> {
        try {
          field.setAccessible(true);
          return field;
        } catch (InaccessibleObjectException e) {
          return null;
        }
      }).filter(Objects::nonNull).collect(Collectors.toList());
    });
  }

  private static Object getFieldValue(Field field, Object from) {
    try {
      return field.get(from);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
