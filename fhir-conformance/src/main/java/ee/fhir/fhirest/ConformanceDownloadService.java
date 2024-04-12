package ee.fhir.fhirest;

import ee.fhir.fhirest.core.api.conformance.ConformanceUpdateListener;
import ee.fhir.fhirest.core.service.conformance.ConformanceHolder;
import jakarta.inject.Inject;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty("fhirest.conformance.definitions-url")
public class ConformanceDownloadService implements ConformanceUpdateListener {
  private boolean singleShot;
  @Inject
  private Provider<ConformanceFileImportService> conformanceFileImportService;
  @Value("${fhirest.conformance.definitions-url}")
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
      File output = new File("/tmp/fhirest-conformance");
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
    log.info("unzipping " + zip.getName() + " to " + outputDir.getAbsolutePath());
    try (ZipFile zipFile = new ZipFile(zip)) {
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
      throw new RuntimeException(e);
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
