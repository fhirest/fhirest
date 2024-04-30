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

package ee.fhir.fhirest.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public final class JsonUtil {
  private static final Gson gson;

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
    return fromJson(new String(json, StandardCharsets.UTF_8));
  }

  public static Stream<Object> fhirpathSimple(String json, String path) {
    return read(fromJson(json), path);
  }

  public static Stream<Object> fhirpathSimple(Object jsonObject, String path) {
    return jsonObject == null ? Stream.empty() : read(jsonObject, path);
  }

  @SuppressWarnings("unchecked")
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
