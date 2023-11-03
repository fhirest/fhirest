package com.kodality.kefhir;

import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Provider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Singleton
@Requires(property = "kefhir.conformance.definitions-url")
public class ConformanceDownloadService implements ConformanceUpdateListener {
  private boolean singleShot;
  @Inject
  private Provider<ConformanceFileImportService> conformanceFileImportService;
  @Value("${kefhir.conformance.definitions-url}")
  private String url;

  @Override
  public void updated() {
    if (StringUtils.isBlank(url)) {
      return;
    }
    if (singleShot) {
      return;
    }
    singleShot = true;
    if (ConformanceHolder.getCapabilityStatement() == null) {
      CompletableFuture.runAsync(() -> importFromUrl(url));
    } else {
      log.info("conformance seems to be initialized. will not download.");
    }
  }

  public void importFromUrl(String url) {
    try {
      File output = new File("/tmp/kefhir-conformance");
      output.mkdirs();
      File zip = downloadZip(url, output);
      unzip(zip, output);
      conformanceFileImportService.get().importFromFile(output.getPath());
    } catch (Exception e) {
      log.error("", e);
    }
  }

  private File downloadZip(String url, File output) {
    try {
      log.info("downloading '" + url + "'");
      Path dest = Paths.get(output.getAbsolutePath() + "/download.zip");
      Files.copy(new URL(url).openStream(), dest, StandardCopyOption.REPLACE_EXISTING);
      return dest.toFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void unzip(File zip, File outputDir) {
    try {
      log.info("unzipping " + zip.getName() + " to " + outputDir.getAbsolutePath());
      ZipFile zipFile = new ZipFile(zip);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        File destination = getZipDestinationFile(outputDir, entry.getName());
        if (destination.isDirectory() || entry.getName().endsWith("/")) {
          destination.mkdirs();
        } else {
          destination.getParentFile().mkdirs();
          zipFile.getInputStream(entry).transferTo(new FileOutputStream(destination));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private File getZipDestinationFile(File outputDir, String fileName) throws IOException {
    File destinationFile = new File(outputDir, fileName);
    String canonicalDestinationFile = destinationFile.getCanonicalPath();
    if (!canonicalDestinationFile.startsWith(outputDir.getCanonicalPath() + File.separator)) {
      throw new RuntimeException("Entry is outside of the target dir: " + fileName);
    }
    return destinationFile;
  }

}
