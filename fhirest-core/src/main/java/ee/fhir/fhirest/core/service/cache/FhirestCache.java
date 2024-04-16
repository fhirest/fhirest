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
