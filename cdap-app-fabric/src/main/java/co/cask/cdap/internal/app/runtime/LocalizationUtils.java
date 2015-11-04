/*
 * Copyright © 2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.internal.app.runtime;

import co.cask.cdap.internal.app.runtime.distributed.LocalizeResource;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utilities for file localization.
 */
public final class LocalizationUtils {
  private static final Logger LOG = LoggerFactory.getLogger(LocalizationUtils.class);

  /**
   * Localizes the specified {@link LocalizeResource} in the specified {@link File targetDir} with the specified
   * file name and returns the {@link File} pointing to the localized file.
   *
   * @param fileName the name to localize the file with
   * @param resource the {@link LocalizeResource} to localize
   * @param targetDir the directory to localize the resource in
   * @return the {@link File} pointing to the localized file.
   */
  public static File localizeResource(String fileName, LocalizeResource resource, File targetDir) throws IOException {
    File tempFile = new File(targetDir, fileName);
    File fileToLocalize = new File(resource.getURI().getPath());
    if (resource.isArchive()) {
      LOG.debug("Uncompress file {} to {}", fileToLocalize, tempFile);
      uncompress(fileToLocalize, tempFile);
    } else {
      LOG.debug("Copy file from {} to {}", fileToLocalize, tempFile);
      Files.copy(fileToLocalize, tempFile);
    }
    return tempFile;
  }

  private static void uncompress(File archive, File targetDir) throws IOException {
    if (!targetDir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      targetDir.mkdir();
    }
    String extension = Files.getFileExtension(archive.getPath());
    switch (extension) {
      case "zip":
        unzip(archive, targetDir);
        break;
      case "jar":
        unzip(archive, targetDir);
        break;
      case "gz":
        // gz is not recommended for archiving multiple files together. So we only support .tar.gz
        untargz(archive, targetDir);
        break;
      case "tar":
        untar(archive, targetDir);
        break;
      default:
        throw new IllegalArgumentException(String.format("Unsupported compression type %s. Only 'zip', 'jar', " +
                                                           "'gz', 'gzip', 'tar', 'tar.gz' are supported.",
                                                         extension));
    }
  }

  private static void unzip(File zipFile, File targetDir) throws IOException {
    final ZipFile zip = new ZipFile(zipFile);
    Enumeration<? extends ZipEntry> zipEntries = zip.entries();
    while (zipEntries.hasMoreElements()) {
      final ZipEntry entry = zipEntries.nextElement();
      // Have to do a new File(file.getName()).getName() to get the file name, because for ZipFile, entry.getName
      // returns the absolute path.
      String outFileName = new File(entry.getName()).getName();
      File outFile = new File(targetDir, outFileName);
      if (entry.isDirectory()) {
        //noinspection ResultOfMethodCallIgnored
        outFile.mkdir();
        continue;
      }
      Files.copy(new InputSupplier<InputStream>() {
        @Override
        public InputStream getInput() throws IOException {
          return zip.getInputStream(entry);
        }
      }, outFile);
    }
  }

  private static void untar(File tarFile, File targetDir) throws IOException {
    try (FileInputStream fis = new FileInputStream(tarFile);
         TarArchiveInputStream tis = new TarArchiveInputStream(fis)) {
      extractTar(tis, targetDir);
    }
  }

  private static void untargz(File tarGzFile, File targetDir) throws IOException {
    try (FileInputStream fis = new FileInputStream(tarGzFile);
         GZIPInputStream gzis = new GZIPInputStream(fis);
         TarArchiveInputStream tis = new TarArchiveInputStream(gzis)) {
      extractTar(tis, targetDir);
    }
  }

  private static void extractTar(final TarArchiveInputStream tis, File targetDir) throws IOException {
    byte[] buffer = new byte[1024];
    TarArchiveEntry entry = tis.getNextTarEntry();
    while (entry != null) {
      File outFile = new File(targetDir, new File(entry.getName()).getName());
      if (entry.isDirectory()) {
        //noinspection ResultOfMethodCallIgnored
        outFile.mkdirs();
        continue;
      }
      try (FileOutputStream fos = new FileOutputStream(outFile)) {
        int len;
        while ((len = tis.read(buffer)) > 0) {
          fos.write(buffer, 0, len);
        }
      }
      entry = tis.getNextTarEntry();
    }
  }

  private LocalizationUtils() {
  }
}
