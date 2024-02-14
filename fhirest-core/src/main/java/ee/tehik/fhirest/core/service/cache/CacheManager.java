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
package ee.tehik.fhirest.core.service.cache;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
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
public class CacheManager implements AutoCloseable {
  private final org.ehcache.CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder().build();

  public CacheManager() {
    manager.init();
  }

  public Cache<String, Object> registerCache(String name, int maxEntries, long ttlSeconds) {
    CacheConfigurationBuilder<String, Object> builder = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(maxEntries));
    builder.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ttlSeconds)));
    if (getCache(name) != null) {
      manager.removeCache(name);
    }
    return manager.createCache(name, builder.build());
  }

  public Cache<String, Object> getCache(String cacheName) {
    return manager.getCache(cacheName, String.class, Object.class);
  }

  @SuppressWarnings("unchecked")
  public <V> V get(String cacheName, String key, Supplier<V> computeFn) {
    Cache<String, Object> cache = getCache(cacheName);
    if (!cache.containsKey(key)) {
      V value = computeFn.get();
      if (value == null) {
        return null;
      }
      cache.put(key, value);
    }
    return (V) cache.get(key);
  }

  public void remove(String cacheName, String key) {
    getCache(cacheName).remove(key);
  }

  @PreDestroy
  @Override
  public void close() {
    log.info("Closing cache manager...");
    this.manager.close();
  }

}
