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

package ee.fhir.fhirest.structure.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ee.fhir.fhirest.structure.api.ParseException;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.api.ResourceRepresentation;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service manages all implemented resource formats (json, xml, ...) for simple resource parsing and composing
 */
@RequiredArgsConstructor
@Component
public class ResourceFormatService {
  private final List<ResourceRepresentation> representations;
  private final ContentTypeService justForBeanInit; //needed
  private static ResourceFormatService instance;

  @Value("${fhirest.resource-formatter.cache.max-size:1000}")
  private long maxCacheSize;
  @Value("${fhirest.resource-formatter.cache.expire-after-write:32}") //in seconds
  private int cacheTtl;
  private Cache<String, Resource> cache;

  @PostConstruct
  private void init() {
    ResourceFormatService.instance = this;
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
        .maximumSize(maxCacheSize)
        .build();
  }

  @PreDestroy
  public void destroy() {
    cache.invalidateAll();
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
    return (R) cache.get(key, k -> guessPresenter(input)
            .orElseThrow(() -> new ParseException("unknown format: [" + StringUtils.left(input, 10) + "]"))
            .parse(input))
        .copy();
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
