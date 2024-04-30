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

package ee.fhir.fhirest.structure.service;

import ee.fhir.fhirest.structure.api.ParseException;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.api.ResourceRepresentation;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
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
    return compose(resource, mimes == null || mimes.length == 0 ? List.of("json") : List.of(mimes));
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
