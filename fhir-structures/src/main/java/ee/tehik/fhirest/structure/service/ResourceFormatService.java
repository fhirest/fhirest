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
package ee.tehik.fhirest.structure.service;

import ee.tehik.fhirest.structure.api.ParseException;
import ee.tehik.fhirest.structure.api.ResourceContent;
import ee.tehik.fhirest.structure.api.ResourceRepresentation;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.hl7.fhir.r5.model.Resource;

@RequiredArgsConstructor
@Singleton
public class ResourceFormatService {
  private final List<ResourceRepresentation> representations;
  private final ContentTypeService justForBeanInit; //needed
  private Cache<String, ? extends Resource> cache;
  private static ResourceFormatService instance;

  @PostConstruct
  private void init() {
    ResourceFormatService.instance = this;
    CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder().build();
    manager.init();
    CacheConfigurationBuilder<String, Resource> builder = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(String.class, Resource.class, ResourcePoolsBuilder.heap(2048));
    builder.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(32)));
    cache = manager.createCache("resources", builder.build());
  }

  @PreDestroy
  public void destroy() {
    cache.clear();
  }

  public static ResourceFormatService get() {
    return instance;
  }

  public ResourceContent compose(Resource resource, String... mimes) {
    //XXX: default json if empty?
    return compose(resource, mimes == null ? List.of("json") : List.of(mimes));
  }

  public ResourceContent compose(Resource resource, List<String> mimes) {
    if (resource == null) {
      return null;
    }
    ResourceRepresentation presenter = findPresenter(mimes).orElseThrow(() -> new ParseException("unknown format"));
    return new ResourceContent(presenter.compose(resource), presenter.getName());
  }

  public <R extends Resource> R parse(ResourceContent content) {
    return parse(content.getValue());
  }

  @SuppressWarnings("unchecked")
  public <R extends Resource> R parse(String input) {
    if (input == null) {
      return null;
    }
    String key = DigestUtils.md5Hex(input);
    if (cache.get(key) == null) {
      cache.put(key,
          guessPresenter(input)
              .orElseThrow(() -> new ParseException("unknown format: [" + StringUtils.left(input, 10) + "]"))
              .parse(input));
    }
    return (R) cache.get(key).copy();
  }

  public List<String> findSupported(List<String> contentTypes) {
    //TODO: why json? find first? ordered?
    return contentTypes.stream().map(ct -> ct.equals("*/*") ? "application/json" : ct).filter(ct -> findPresenter(ct).isPresent()).toList();
  }

  public Optional<ResourceRepresentation> findPresenter(List<String> contentTypes) {
    return contentTypes.stream().map(m -> findPresenter(m).orElse(null)).filter(Objects::nonNull).findFirst();
  }

  public Optional<ResourceRepresentation> findPresenter(String contentType) {
    if (contentType == null) {
      return Optional.empty();
    }
    String mime = StringUtils.substringBefore(contentType, ";");
    return representations.stream().filter(c -> c.getMimeTypes().contains(mime)).findFirst();
  }

  private Optional<ResourceRepresentation> guessPresenter(String content) {
    return representations.stream().filter(c -> c.isParsable(content)).findFirst();
  }

}
