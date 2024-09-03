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

package ee.fhir.fhirest.hashchain;

import ee.fhir.fhirest.core.api.resource.ResourceAfterSaveInterceptor;
import ee.fhir.fhirest.core.model.ResourceVersion;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class HashchainInterceptor extends ResourceAfterSaveInterceptor {
  private final ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
  @Inject
  private HashchainRepository hashchainRepository;

  public HashchainInterceptor() {
    super(ResourceAfterSaveInterceptor.FINALIZATION);
  }

  @Override
  public void handle(ResourceVersion version) {
    CompletableFuture.runAsync(() -> storeHashchain(version), singleThreadPool);
  }

  private void storeHashchain(ResourceVersion version) {
    try {
      hashchainRepository.storeHashchain(version.getId());
    } catch (Exception e) {
      log.error("hashchain saver:", e);
      throw e;
    }
  }
}
