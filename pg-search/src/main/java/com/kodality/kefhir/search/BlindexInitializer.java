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
package com.kodality.kefhir.search;

import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.search.repository.BlindexRepository;
import com.kodality.kefhir.search.util.FhirPathHackUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PSQLException;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class BlindexInitializer implements ConformanceUpdateListener {
  private final BlindexRepository blindexRepository;

  @Override
  public void updated() {
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      execute();
    });
  }

  public Object execute() {
    List<String> defined = ConformanceHolder.getDefinitions().stream().map(def -> def.getName()).collect(Collectors.toList());
    if (CollectionUtils.isEmpty(defined)) {
      log.error("blindex: will not run. definitions either empty, either definitions not yet loaded.");
      return null;
    }
    Set<String> create =
        ConformanceHolder.getSearchParams().stream().filter(sp -> sp.getExpression() != null).flatMap(sp -> {
          return Stream.of(StringUtils.split(sp.getExpression(), "|"))
              .map((s) -> StringUtils.trim(s))
              .map(s -> FhirPathHackUtil.replaceAs(s))
              .filter(s -> defined.contains(StringUtils.substringBefore(s, ".")));
        }).collect(Collectors.toSet());

    Set<String> current = blindexRepository.load().stream().map(i -> i.getKey()).collect(Collectors.toSet());
    Set<String> drop = new HashSet<>(current);
    drop.removeAll(create);
    create.removeAll(current);
    log.debug("currently indexed: " + current);
    log.debug("need to create: " + create);
    log.debug("need to remove: " + drop);
    create(create);
    drop(drop);
    blindexRepository.init();
    log.info("blindex initialization finished");
    return null;
  }

  private void create(Set<String> create) {
    List<String> errors = new ArrayList<>();
    for (String key : create) {
      try {
        blindexRepository.createIndex(StringUtils.substringBefore(key, "."), StringUtils.substringAfter(key, "."));
      } catch (Exception e) {
        String err = e.getMessage();
        if (e.getCause() instanceof PSQLException) {
          err = (e.getCause().getMessage().substring(0, e.getCause().getMessage().indexOf("\n")));
        }
        log.info("failed " + key + ": " + err);
        errors.add(key + ": " + err);
      }
    }
    log.error("failed to create " + errors.size() + " indexes");
  }

  private void drop(Set<String> drop) {
    for (String key : drop) {
      try {
        blindexRepository.dropIndex(StringUtils.substringBefore(key, "."), StringUtils.substringAfter(key, "."));
      } catch (Exception e) {
        String err = e.getCause() instanceof PSQLException ? e.getCause().getMessage() : e.getMessage();
        log.debug("failed " + key + ": " + err);
      }
    }
  }

}
