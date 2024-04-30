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

package ee.fhir.fhirest.core.service.cache;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.ehcache.Cache;

public class FhirestCache {
  private final Cache<String, Object> cache;

  public FhirestCache(Cache<String, Object> cache) {
    this.cache = cache;
  }

  @SuppressWarnings("unchecked")
  public <V> V get(String key, Supplier<V> computeFn) {
    if (!cache.containsKey(key)) {
      V value = computeFn.get();
      if (value == null) {
        return null;
      }
      cache.put(key, value);
    }
    return (V) cache.get(key);
  }

  @SuppressWarnings("unchecked")
  public synchronized <V> CompletableFuture<V> getCf(String key, Supplier<CompletableFuture<V>> computeFn) {
    CompletableFuture<V> value = (CompletableFuture<V>) cache.get(key);
    if (value == null || value.isCancelled() || value.isCompletedExceptionally()) {
      value = computeFn.get();
      value.thenAccept(v -> cache.put(key, CompletableFuture.completedFuture(v)));
      cache.put(key, value);
    }
    return value.copy();
  }

  public void remove(String key) {
    cache.remove(key);
  }
}
