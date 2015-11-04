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

import co.cask.cdap.common.io.Locations;
import co.cask.cdap.common.lang.jar.BundleJarUtil;
import co.cask.cdap.internal.app.runtime.distributed.LocalizeResource;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tests for {@link LocalizationUtils}.
 */
public class LocalizationUtilsTest {
  @ClassRule
  public static final TemporaryFolder TEMP_FOLDER = new TemporaryFolder();

  @Test
  public void testZip() throws IOException {
    String zipFileName = "target";
    File directory = TEMP_FOLDER.newFolder("zip");
    File file1 = File.createTempFile("file1", ".txt", directory);
    File file2 = File.createTempFile("file2", ".txt", directory);
    File zipFile = createZipFile(zipFileName, file1, file2);
    File localizationDir = TEMP_FOLDER.newFolder("localZip");
    File localizedResource = LocalizationUtils.localizeResource(zipFileName, new LocalizeResource(zipFile, true),
                                                                localizationDir);
    Assert.assertTrue(localizedResource.isDirectory());
    File[] files = localizedResource.listFiles();
    Assert.assertNotNull(files);
    Assert.assertEquals(2, files.length);
    if (file1.getName().equals(files[0].getName())) {
      Assert.assertEquals(file2.getName(), files[1].getName());
    } else {
      Assert.assertEquals(file1.getName(), files[1].getName());
      Assert.assertEquals(file2.getName(), files[0].getName());
    }
  }

  @Test
  public void testJar() throws IOException {
    String jarFileName = "target";
    File directory = TEMP_FOLDER.newFolder("jar");
    File libDir = new File(directory, "lib");
    Assert.assertTrue(libDir.mkdirs());
    File someClassFile = File.createTempFile("SomeClass", ".class", directory);
    File someOtherClassFile = File.createTempFile("SomeOtherClass", ".class", directory);
    File jarFile = createJarFile(jarFileName, directory);
    File localizationDir = TEMP_FOLDER.newFolder("localJar");
    File localizedResource = LocalizationUtils.localizeResource(jarFileName, new LocalizeResource(jarFile, true),
                                                                localizationDir);
    Assert.assertTrue(localizedResource.isDirectory());
    File[] files = localizedResource.listFiles();
    Assert.assertNotNull(files);
    Assert.assertEquals(3, files.length);
    for (File file : files) {
      String name = file.getName();
      if (libDir.getName().equals(name)) {
        Assert.assertTrue(file.isDirectory());
      } else {
        Assert.assertTrue(someClassFile.getName().equals(name) || someOtherClassFile.getName().equals(name));
      }
    }
  }

  @Test
  public void testTar() throws IOException {
    testTarFiles(false);
  }

  @Test
  public void testTarGz() throws IOException {
    testTarFiles(true);
  }

  private void testTarFiles(boolean gzipped) throws IOException {
    String tarFileName = "target";
    // Have to use short file/directory names because TarArchiveOutputStream does not like long paths.
    File directory = gzipped ? TEMP_FOLDER.newFolder("t2") : TEMP_FOLDER.newFolder("t1");
    File file1 = new File(Files.createFile(Paths.get(new File(directory, "f1").toURI())).toUri());
    File file2 = new File(Files.createFile(Paths.get(new File(directory, "f2").toURI())).toUri());
    File tarFile = gzipped ? createTarGzFile(tarFileName, file1, file2) : createTarFile(tarFileName, file1, file2);
    File localizationDir = gzipped ? TEMP_FOLDER.newFolder("localTarGz") : TEMP_FOLDER.newFolder("localTar");
    File localizedResource = LocalizationUtils.localizeResource(tarFileName, new LocalizeResource(tarFile, true),
                                                                localizationDir);
    Assert.assertTrue(localizedResource.isDirectory());
    File[] files = localizedResource.listFiles();
    Assert.assertNotNull(files);
    Assert.assertEquals(2, files.length);
    for (File file : files) {
      String name = file.getName();
      Assert.assertTrue(file1.getName().equals(name) || file2.getName().equals(name));
    }
  }

  private File createZipFile(String zipFileName, File ... filesToAdd) throws IOException {
    File target = TEMP_FOLDER.newFile(zipFileName + ".zip");
    try (FileOutputStream fos = new FileOutputStream(target);
         ZipOutputStream out = new ZipOutputStream(fos)) {
      for (File file : filesToAdd) {
        out.putNextEntry(new ZipEntry(file.getAbsolutePath()));
        com.google.common.io.Files.copy(file, out);
        out.closeEntry();
      }
    }
    return target;
  }

  private static File createJarFile(String jarFileName, File dirToJar) throws IOException {
    File target = TEMP_FOLDER.newFile(jarFileName + ".jar");
    BundleJarUtil.createJar(dirToJar, target);
    File[] files = dirToJar.listFiles();
    Assert.assertNotNull(files);
    for (File file : files) {
      if (!file.isDirectory()) {
        BundleJarUtil.getEntry(Locations.toLocation(target), file.getName()).getInput().close();
      }
    }
    return target;
  }

  private static File createTarFile(String tarFileName, File ... filesToAdd) throws IOException {
    File target = TEMP_FOLDER.newFile(tarFileName + ".tar");
    try (FileOutputStream fos = new FileOutputStream(target);
         BufferedOutputStream bos = new BufferedOutputStream(fos);
         TarArchiveOutputStream tos = new TarArchiveOutputStream(bos)) {
      addFilesToTar(tos, filesToAdd);
    }
    return target;
  }

  private static File createTarGzFile(String gzipFileName, File ... filesToAdd) throws IOException {
    File target = TEMP_FOLDER.newFile(gzipFileName + ".tar.gz");
    try (FileOutputStream fos = new FileOutputStream(target);
         BufferedOutputStream bos = new BufferedOutputStream(fos);
         GZIPOutputStream gzos = new GZIPOutputStream(bos);
         TarArchiveOutputStream tos = new TarArchiveOutputStream(gzos)) {
      addFilesToTar(tos, filesToAdd);
    }
    return target;
  }

  private static void addFilesToTar(TarArchiveOutputStream tos, File ... filesToAdd) throws IOException {
    for (File file : filesToAdd) {
      TarArchiveEntry tarEntry = new TarArchiveEntry(file);
      tos.putArchiveEntry(tarEntry);
      if (file.isFile()) {
        com.google.common.io.Files.copy(file, tos);
      }
      tos.closeArchiveEntry();
    }
  }
}
