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
