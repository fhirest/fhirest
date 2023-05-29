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
package com.kodality.kefhir.structure.service;

import com.kodality.kefhir.structure.api.ParseException;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.api.ResourceRepresentation;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
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
  public void destroy(){
    cache.clear();
  }

  public static ResourceFormatService get() {
    return instance;
  }

  public ResourceContent compose(Resource resource, String mime) {
    if (resource == null) {
      return null;
    }
    ResourceRepresentation presenter =
        findPresenter(mime).orElse(findPresenter("json").orElseThrow(() -> new ParseException("unknown format")));
    return new ResourceContent(presenter.compose(resource), presenter.getFhirFormat().getExtension());
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

  public Optional<ResourceRepresentation> findPresenter(String ct) {
    if (ct == null) {
      return Optional.empty();
    }
    String mime = StringUtils.substringBefore(ct, ";");
    return representations.stream().filter(c -> c.getMimeTypes().contains(mime)).findFirst();
  }

  private Optional<ResourceRepresentation> guessPresenter(String content) {
    return representations.stream().filter(c -> c.isParsable(content)).findFirst();
  }

}
