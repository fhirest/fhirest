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
 package com.kodality.kefhir.core.service.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import static java.util.stream.Collectors.toSet;

@Singleton
public class CacheManager {
  private final org.ehcache.CacheManager manager;
  private final Map<String, Cache<String, Object>> cacheHolder = new HashMap<>();

  public CacheManager() {
    CacheManagerBuilder<org.ehcache.CacheManager> builder = CacheManagerBuilder.newCacheManagerBuilder();
    manager = builder.build();
  }

  @PostConstruct
  private void init() {
    manager.init();
  }

  public Cache<String, Object> registerCache(String name, int maxEntries, long ttlSeconds) {
    CacheConfigurationBuilder<String, Object> builder = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(maxEntries));
    builder.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ttlSeconds)));
    if(cacheHolder.containsKey(name)) {
      manager.removeCache(name);
    }
    Cache<String, Object> cache = manager.createCache(name, builder.build());
    cacheHolder.put(name, cache);
    return cache;
  }

  @SuppressWarnings("unchecked")
  public <V> V get(String cacheName, String key, Supplier<V> computeFn) {
    Cache<String, Object> cache = cacheHolder.get(cacheName);
    if (!cache.containsKey(key)) {
      V value = computeFn.get();
      if (value == null) {
        return null;
      }
      cache.put(key, value);
    }
    return (V) cache.get(key);
  }

  public Cache<String, Object> getCache(String cacheName) {
    return cacheHolder.get(cacheName);
  }

  public Set<String> getKeys(String cacheName) {
    Cache<String, Object> cache = getCache(cacheName);
    Set<String> keys = new HashSet<>();
    cache.forEach(e -> keys.add(e.getKey()));
    return keys;
  }

  public void removeKeys(String cacheName, String keyStart) {
    Cache<String, Object> cache = getCache(cacheName);
    cache.removeAll(getKeys(cacheName).stream().filter(k -> k.startsWith(keyStart)).collect(toSet()));
  }

}
