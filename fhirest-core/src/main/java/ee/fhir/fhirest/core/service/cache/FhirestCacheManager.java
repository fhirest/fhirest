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
package ee.fhir.fhirest.core.service.cache;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FhirestCacheManager implements AutoCloseable {
  private final org.ehcache.CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder().build();
  private final Map<String, FhirestCache> caches = new HashMap<>();

  public FhirestCacheManager() {
    manager.init();
  }

  public FhirestCache registerCache(String name, int maxEntries, long ttlSeconds) {
    CacheConfigurationBuilder<String, Object> builder = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(maxEntries));
    builder.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ttlSeconds)));
    if (getCache(name) != null) {
      manager.removeCache(name);
    }
    Cache<String, Object> cache = manager.createCache(name, builder.build());
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
    log.info("Closing cache manager...");
    this.manager.close();
  }

}
