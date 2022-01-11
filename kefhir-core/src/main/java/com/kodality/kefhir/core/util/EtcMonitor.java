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
 package com.kodality.kefhir.core.util;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class EtcMonitor {
  private FileAlterationMonitor monitor;
  protected final Path path;

  public EtcMonitor(String dir) {
    String p = StringUtils.join(new String[] { ".", "etc", dir }, File.separatorChar) + File.separatorChar;
    path = FileSystems.getDefault().getPath(p);
  }

  protected abstract void file(File file);// file

  protected abstract void clear();

  protected void finish() {
    // overwrite
  }

  protected void start() {
    if (!path.toFile().exists()) {
      log.warn(path + " does not exist. Exiting monitor");
      return;
    }
    FileAlterationObserver fao = new FileAlterationObserver(path.toFile());
    fao.addListener(new Monitor());
    monitor = new FileAlterationMonitor(10001, fao);
    try {
      monitor.start();
    } catch (Exception e) {
      log.error(path.toString(), e);
      throw new RuntimeException(e);
    }
  }

  protected void stop() {
    if (monitor == null) {
      return;
    }
    try {
      monitor.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void read(File cat) {
    if (cat.isFile()) {
      try {
        file(cat);
      } catch (Exception e) {
        log.error("", e);
        throw e;
      }
      return;
    }
    if (cat.isDirectory()) {
      Stream.of(cat.listFiles()).forEach(this::read);
    }
  }

  private class Monitor extends FileAlterationListenerAdaptor {
    private boolean пириделывай = true;

    @Override
    public void onFileCreate(File file) {
      пириделывай = true;
    }

    @Override
    public void onFileChange(File file) {
      пириделывай = true;
    }

    @Override
    public void onFileDelete(File file) {
      пириделывай = true;
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
      if (!пириделывай) {
        return;
      }
      clear();
      read(path.toFile());
      finish();
      пириделывай = false;// пириделал!
    }
  }

}
