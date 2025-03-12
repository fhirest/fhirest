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

package ee.fhir.fhirest;

import ee.fhir.fhirest.core.service.conformance.ConformanceInitializationService;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConformanceFileImportService {
  private final ConformanceSaveService conformanceSaveService;
  private final ConformanceInitializationService conformanceService;

  public void importFromFile(String path) {
    log.info("Loading initial conformance data. This may take several minutes.");
    readDir(new File(path));
    conformanceService.refresh();
  }

  private void readDir(File dir) {
    Arrays.stream(dir.listFiles()).parallel().forEach(f -> {
      if (f.isDirectory()) {
        if (f.getName().equals("__MACOSX")) {
          return;
        }
        readDir(f);
      }
      process(f);
    });
  }

  private void process(File f) {
    if (!f.getName().endsWith(".json")) {
      return;
    }
    log.info("processing " + f);
    Resource r = readFile(f);
    Date modified = new Date(f.lastModified());
    if (r instanceof Bundle b) {
      Set<String> removeDuplicates = new HashSet<>(); // any why are there any?
      b.getEntry().stream()
          .filter(e -> removeDuplicates.add(r.getResourceType().name() + "/" + e.getResource().getId()))
          /*.parallel()*/.forEach(e -> conformanceSaveService.save(e.getResource(), modified));
    } else {
      conformanceSaveService.save(r, modified);
    }
  }

  public Resource readFile(File f) {
    try {
      InputStream is = new FileInputStream(f);
      return new JsonParser().parse(is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
