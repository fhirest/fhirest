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

package ee.fhir.fhirest.core.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FhirestCacheManager implements AutoCloseable {
  private final Map<String, FhirestCache> caches = new HashMap<>();

  public FhirestCache registerCache(String name, int maxEntries, long ttlSeconds) {
    Cache<String, Object> cache =  Caffeine.newBuilder()
        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
        .maximumSize(maxEntries)
        .build();
    FhirestCache fhirestCache = new FhirestCache(cache);
    caches.put(name, fhirestCache);
    return fhirestCache;
  }

  public FhirestCache getCache(String cacheName) {
    return caches.get(cacheName);
  }

  public <V> V get(String cacheName, String key, Supplier<V> computeFn) {
    return caches.get(cacheName).get(key, computeFn);
  }

  public <V> CompletableFuture<V> getCf(String cacheName, String key, Supplier<CompletableFuture<V>> computeFn) {
    return caches.get(cacheName).getCf(key, computeFn);
  }

  public void remove(String cacheName, String key) {
    caches.get(cacheName).remove(key);
  }

  @PreDestroy
  @Override
  public void close() {
    this.caches.clear();
  }

}
