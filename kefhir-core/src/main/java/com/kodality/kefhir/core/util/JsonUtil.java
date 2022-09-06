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
package com.kodality.kefhir.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public final class JsonUtil {
  private static Gson gson;

  static {
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Double.class, new WriteDoubleAsInt());
    gson = builder.create();
  }

  private JsonUtil() {
    //
  }

  public static String toJson(Object o) {
    return o == null ? null : gson.toJson(o);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> fromJson(String json) {
    return gson.fromJson(json, Map.class);
  }

  public static Map<String, Object> fromJson(byte[] json) {
    try {
      return fromJson(new String(json, "UTF8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static Stream<Object> fhirpathSimple(String json, String path) {
    return read(fromJson(json), path);
  }

  public static Stream<Object> fhirpathSimple(Object jsonObject, String path) {
    return jsonObject == null ? Stream.empty() : read(jsonObject, path);
  }

  private static Stream<Object> read(Object jsonObject, String path) {
    if (jsonObject == null) {
      return Stream.empty();
    }
    Stream<Object> stream = jsonObject instanceof List ? ((List<Object>) jsonObject).stream() : Stream.of(jsonObject);
    if (StringUtils.isEmpty(path)) {
      return stream;
    }
    String child = StringUtils.substringBefore(path, ".");
    String chain = StringUtils.substringAfter(path, ".");
    return stream.flatMap(el -> {
      if (!(el instanceof Map)) {
        throw new RuntimeException("incorrect path " + path);
      }
      return read(((Map<String, Object>) el).get(child), chain);
    });
  }

  private static class WriteDoubleAsInt implements JsonSerializer<Double> {

    @Override
    public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
      return src == src.longValue() ? new JsonPrimitive(src.longValue()) : new JsonPrimitive(src);
    }
  }
}
