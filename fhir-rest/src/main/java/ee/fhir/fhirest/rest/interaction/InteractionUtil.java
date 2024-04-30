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

package ee.fhir.fhirest.rest.interaction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class InteractionUtil {
  private static final Map<Method, String> interactionCache = new ConcurrentHashMap<>();

  private InteractionUtil() {
    //
  }

  public static List<Method> findAllMethods(Class<?> clazz) {
    return Arrays.stream(clazz.getMethods()).filter(m -> getMethodInteraction(m) != null).collect(Collectors.toList());
  }

  public static List<Method> findMethods(String interaction, Class<?> clazz) {
    if (interaction == null) {
      return Collections.emptyList();
    }
    try {
      List<Method> result = new ArrayList<>();
      for (Method m : clazz.getMethods()) {
        if (interaction.equals(getMethodInteraction(m))) {
          result.add(m);
        }
      }
      return result;
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getMethodInteraction(Method method) {
    return interactionCache.computeIfAbsent(method, (m) -> {
      if (m.isAnnotationPresent(FhirInteraction.class)) {
        return m.getAnnotation(FhirInteraction.class).interaction();
      }

      for (Class<?> iface : m.getDeclaringClass().getInterfaces()) {
        try {
          String result = getMethodInteraction(iface.getMethod(m.getName(), m.getParameterTypes()));
          if (result != null) {
            return result;
          }
        } catch (NoSuchMethodException e) {
          //continue
        }
      }

      Class<?> superClass = m.getDeclaringClass().getSuperclass();
      if (superClass != null) {
        try {
          String result = getMethodInteraction(superClass.getMethod(m.getName(), m.getParameterTypes()));
          if (result != null) {
            return result;
          }
        } catch (NoSuchMethodException e) {
          //continue
        }
      }

      return null;
    });
  }

}
