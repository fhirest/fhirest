package com.kodality.kefhir;

import com.kodality.kefhir.core.service.conformance.ConformanceInitializationService;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Resource;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class ConformanceFileImportService {
  private final ConformanceSaveService conformanceSaveService;
  private final ResourceFormatService formatService;
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
      byte[] content = IOUtils.toByteArray(is);
      String json = asString(content);
      return formatService.parse(json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected String asString(byte[] content) {
    if (content == null) {
      return null;
    }
    try {
      return new String(content, "UTF8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
