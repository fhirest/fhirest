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
package ee.tehik.fhirest;

import ee.tehik.fhirest.core.service.conformance.ConformanceInitializationService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import lombok.RequiredArgsConstructor;

//TODO: auth. check resource types?
@Controller("/conformance-tools")
@RequiredArgsConstructor
public class ConformanceToolsController {
  private final ConformanceFileImportService conformanceFileImportService;
  private final ConformanceDownloadService conformanceDownloadService;
  private final ConformanceInitializationService conformanceInitializationService;

  @Post("/import-file")
  public HttpResponse<?> importFile(@QueryValue String file) {
    conformanceFileImportService.importFromFile(file);
    return HttpResponse.ok();
  }

  @Post("/import-url")
  public HttpResponse<?> importUrl(@QueryValue String url) {
    conformanceDownloadService.importFromUrl(url);
    return HttpResponse.ok();
  }

  @Post("/refresh")
  public HttpResponse<?> refreshConformance() {
    conformanceInitializationService.refresh();
    return HttpResponse.ok();
  }
}
