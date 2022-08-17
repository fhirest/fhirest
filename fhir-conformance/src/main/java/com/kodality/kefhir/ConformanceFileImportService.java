package com.kodality.kefhir;

import com.kodality.kefhir.core.api.resource.ResourceAfterSaveInterceptor;
import com.kodality.kefhir.core.api.resource.ResourceStorage;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.VersionId;
import com.kodality.kefhir.core.service.conformance.ConformanceInitializationService;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class ConformanceFileImportService {
  private final ResourceStorage storehouse;
  private final List<ResourceAfterSaveInterceptor> afterSaveInterceptors;
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
          /*.parallel()*/.forEach(e -> save(e.getResource(), modified));
    } else {
      save(r, modified);
    }
  }

  private void save(Resource r, Date fileModified) {
    ResourceId resourceId = new ResourceId(r.getResourceType().name(), r.getId());
    ResourceVersion current = storehouse.load(new VersionId(resourceId));
    if (current == null || current.getModified().before(fileModified)) {
      ResourceVersion version = storehouse.save(resourceId, formatService.compose(r, "json"));
      afterSaveInterceptors.stream().filter(i -> i.getPhase().equals(ResourceAfterSaveInterceptor.TRANSACTION)).forEach(i -> i.handle(version));
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
