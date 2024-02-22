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
package ee.fhir.fhirest;

import ee.fhir.fhirest.core.service.conformance.ConformanceInitializationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

//TODO: auth. check resource types?
@Controller()
@RequestMapping("/conformance-tools")
@RequiredArgsConstructor
public class ConformanceToolsController {
  private final ConformanceFileImportService conformanceFileImportService;
  private final ConformanceDownloadService conformanceDownloadService;
  private final ConformanceInitializationService conformanceInitializationService;

  @PostMapping("/import-file")
  public ResponseEntity<?> importFile(@RequestParam String file) {
    conformanceFileImportService.importFromFile(file);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/import-url")
  public ResponseEntity<?> importUrl(@RequestParam String url) {
    conformanceDownloadService.importFromUrl(url);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refreshConformance() {
    conformanceInitializationService.refresh();
    return ResponseEntity.ok().build();
  }
}
