/**

The MIT License (MIT)

Copyright (c) 2020, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.wfv.forms;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wfv.config.ConfigurationKey;
import com.surftools.wfv.config.IConfigurationManager;
import com.surftools.wfv.tools.Utils;

import net.lingala.zip4j.ZipFile;

public class FormUtils {

  static class FormVersion {
    String longVersion;
    String shortVersion;

    @Override
    public String toString() {
      return "{long: " + longVersion + ", short: " + shortVersion + ":";
    }

    FormVersion(String dirName) throws Exception {
      Path versionPath = Path.of(dirName, "Standard_Forms_Version.dat");
      File versionFile = versionPath.toFile();
      if (versionFile.exists()) {
        longVersion = Files.readString(versionPath).trim();
        shortVersion = longVersion.substring(0, longVersion.lastIndexOf(".")).replace(".", "");
      } else {
        throw new RuntimeException("could not find file: " + versionPath.toAbsolutePath());
      }
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(FormUtils.class);

  private final IConfigurationManager cm;

  private final String formsDirName;
  private final File formsDir;
  private final Path formsPath;

  private FormVersion currentVersion;
  private FormVersion newVersion;

  public FormUtils(IConfigurationManager cm) throws Exception {
    this.cm = cm;

    formsDirName = cm.getAsString(ConfigurationKey.FORMS_PATH);
    formsDir = new File(formsDirName);
    formsPath = Path.of(formsDir.getCanonicalPath());

    boolean needsInitialDownload = false;

    if (!formsDir.exists()) {
      logger.warn("Forms directory: " + formsDirName + " not found. Creating.");
      boolean makeOk = formsDir.mkdirs();
      if (!makeOk) {
        Utils.fatal(cm, ConfigurationKey.EMSG_CANT_MAKE_FORMS_DIR, formsDirName);
      }
      needsInitialDownload = true;
    }

    if (!formsDir.isDirectory()) {
      String timestamp = Utils.makeTimestamp();
      File renamedTo = new File(formsDirName + "-" + timestamp);
      logger.warn("Forms directory: " + formsDirName + " not a directory. Renamed to: " + renamedTo.getName());
      formsDir.renameTo(renamedTo);
      needsInitialDownload = true;
    }

    if (needsInitialDownload) {
      updateForms();
    }

    currentVersion = new FormVersion(Path.of(formsDirName, "StandardForms").toString());
    logger
        .info("Forms path: " + formsDir + ", long version: " + currentVersion.longVersion + ", short version: "
            + currentVersion.shortVersion);
  }

  /**
   * attempt to download
   */
  public void updateForms() {
    // attempt to download
    var ok = downloadForms();
    if (!ok) {
      // if we can't download, we can't do anything
      return;
    }

    if (!currentVersion.shortVersion.equals(newVersion.shortVersion)) {
      boolean okToContinue = Utils
          .promptForBoolean("Update currently installed forms (version " + currentVersion.shortVersion
              + ") with new version (" + newVersion.shortVersion + ")? Default [no]: ");
      if (!okToContinue) {
        logger.info("skipping forms update");
        return;
      }

      try {
        var linkPath = Path.of(formsPath.toString(), "StandardForms");
        Files.delete(linkPath);
        var targetPath = Path.of(formsPath.toString(), "StandardForms-" + newVersion.shortVersion);
        Files.createSymbolicLink(linkPath, targetPath);
      } catch (Exception e) {
        logger.error("Exception unlinking/relinking: " + e.getLocalizedMessage());
      }

      logger.info("StandardForms updated to " + newVersion.shortVersion);
    } else {
      logger.info("StandardForms already at " + newVersion.shortVersion);
    }
  }

  /**
   * download the latest forms from the Winlink site
   *
   * @return true if success, false otherwise
   */
  @SuppressWarnings("resource")
  private boolean downloadForms() {
    var downloadUrl = cm.getAsString(ConfigurationKey.FORMS_DOWNLOAD_URL);
    try {
      HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
      HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      byte[] bytes = response.body();

      if (bytes != null && bytes.length > 1_000_000) {
        logger.info("downloaded new forms: " + bytes.length + " bytes");

        // write the bytes to a file
        Path zipPath = Path.of(formsDir.getCanonicalPath(), "tmp-StandardForms.zip");
        File zipFile = zipPath.toFile();
        FileOutputStream fos = new FileOutputStream(zipFile);
        fos.write(bytes);
        fos.close();

        // unzip
        var tmpDirName = Path.of(formsDir.getCanonicalPath(), "tmp-StandardForms").toString();
        var tmpDir = new File(tmpDirName);
        boolean makeOk = tmpDir.mkdirs();
        if (!makeOk) {
          Utils.fatal(cm, ConfigurationKey.EMSG_CANT_MAKE_FORMS_DIR, formsDirName);
        }
        new ZipFile(zipFile).extractAll(tmpDirName);

        // extract new version
        newVersion = new FormVersion(tmpDirName);

        // rename zip
        String renameZipFileName = Path
            .of(formsPath.toString(), "StandardForms-" + newVersion.longVersion + ".zip")
              .toString();
        File renameZipFile = new File(renameZipFileName);
        zipFile.renameTo(renameZipFile);
        logger.info("wrote zip file to: " + renameZipFile.getName());

        // rename tmpDir
        var renameTmpDir = new File(formsDir.getCanonicalPath(), "StandardForms-" + newVersion.shortVersion);
        tmpDir.renameTo(renameTmpDir);
        logger.info("wrote zipped forms file to: " + renameTmpDir.getName());

      }
      return true;
    } catch (Exception e) {
      logger.error("Exception downloading froms from : " + downloadUrl + ", " + e.getMessage(), e);
      return false;
    }
  }

  /**
   * return canonical pathname for form; given form name
   *
   * @param displayFormName
   * @return
   * @throws Exception
   *           if not found or more than one matching name found
   */
  public String findFormFile(String displayFormName) throws Exception {
    List<Path> matchingPaths = new ArrayList<>();
    Files.walk(Paths.get(formsDir.getCanonicalFile().toURI())).filter(Files::isRegularFile).forEach((f) -> {
      String fileName = f.getFileName().toString();
      if (fileName.equals(displayFormName)) {
        matchingPaths.add(f);
        logger.debug("found matching filename: " + fileName);
      }
    });

    if (matchingPaths.size() > 1) {
      Utils.fatal(cm, ConfigurationKey.EMSG_MULTIPLE_FORM_FILES_FOUND, displayFormName);
    }

    if (matchingPaths.size() == 0) {
      Utils.fatal(cm, ConfigurationKey.EMSG_NO_FORM_FILE_FOUND, displayFormName);
    }

    return matchingPaths.get(0).toFile().getCanonicalPath();
  }

}
