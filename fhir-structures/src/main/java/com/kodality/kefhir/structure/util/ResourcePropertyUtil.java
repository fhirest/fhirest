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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class ResourcePropertyUtil {
  private ResourcePropertyUtil() {
    //
  }

  public static <T> Stream<T> findProperties(Object object, Class<T> fieldClazz) {
    return findProperties(object, new HashSet<>(), fieldClazz);
  }

  @SuppressWarnings("unchecked")
  public static <T> Stream<T> findProperties(Object object, Set<Object> exclude, Class<T> fieldClazz) {
    if (object == null) {
      return Stream.empty();
    }
    if (fieldClazz.equals(object.getClass())) {
      return Stream.of((T) object);
    }
    Field[] fields = FieldUtils.getAllFields(object.getClass());
    return Stream.of(fields).flatMap(field -> {
      try {
        field.setAccessible(true);
      } catch (InaccessibleObjectException e) {
        return Stream.empty();
      }
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

  private static Object getFieldValue(Field field, Object from) {
    try {
      return field.get(from);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
