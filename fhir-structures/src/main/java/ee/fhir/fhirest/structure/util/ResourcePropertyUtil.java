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

package ee.fhir.fhirest.structure.util;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class ResourcePropertyUtil {
  private static final Map<Class<?>, List<Field>> fieldsCache = new ConcurrentHashMap<>();

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
      if (!obj.getClass().getName().startsWith("org.hl7")) {
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
