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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wfv.config.ConfigurationKey;
import com.surftools.wfv.config.IConfigurationManager;
import com.surftools.wfv.tools.Utils;

import net.lingala.zip4j.ZipFile;

public class OldFormUtils {

  private static final Logger logger = LoggerFactory.getLogger(OldFormUtils.class);

  // this is the format that is in StandardForms/Standard_Forms_Version.dat
  private static final String LAST_KNOWN_LONG_VERSION = "1.0.141.0";

  private final IConfigurationManager cm;
  private final File formsDir;
  private String longVersion = LAST_KNOWN_LONG_VERSION;
  private String shortVersion;
  private String updateURL;
  private boolean formsAlreadyUpdated = false;

  public OldFormUtils(IConfigurationManager cm) throws Exception {
    this.cm = cm;

    String formsDirName = cm.getAsString(ConfigurationKey.FORMS_PATH);

    boolean needsInitialDownload = false;
    formsDir = new File(formsDirName);

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

    Path versionPath = Paths.get(formsDirName, "Standard_Forms_Version.dat");
    File versionFile = versionPath.toFile();
    if (versionFile.exists()) {
      longVersion = Files.readString(versionPath).trim();
    }

    // this chops off the last dotted decimal and removes all dots
    // this is the format for getting versions
    shortVersion = makeShortVersion(longVersion);

    logger.info("Forms path: " + formsDir + ", long version: " + longVersion + ", short version: " + shortVersion);
  }

  /**
   * this chops off the last dotted decimal and removes all dots
   *
   * @param s
   * @return
   */
  String makeShortVersion(String s) {
    return s.substring(0, s.lastIndexOf(".")).replace(".", "");
  }

  /**
   * get the latest version available
   *
   * SIDE EFFECT: sets updateURL
   *
   * @return
   */
  public String getRemoteLongVersion() {
    String remoteVersion = null;

    // https://winlink.org/content/how_manually_update_standard_templates_version_10142
    String urlPrefix = cm.getAsString(ConfigurationKey.FORMS_UPDATE_URL_PREFIX);

    // String localVersion = getVersion(); // 10141
    // String requestUriString = urlPrefix + localVersion;
    String requestUriString = urlPrefix;
    logger.info("checking for new form version via: " + requestUriString);

    try {
      HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();

      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUriString)).build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseUriString = response.uri().toURL().toString();
      String responseBody = response.body();
      remoteVersion = findRemoteVersion(responseBody);
      updateURL = findUpdateURL(responseBody);
      logger
          .debug(
              "remoteVersion: " + remoteVersion + ", updateURL: " + updateURL + ", responseURI: " + responseUriString);
    } catch (Exception e) {
      logger.error("Error in isFormsUpdateAvailable(): " + e.getMessage(), e);
    }
    return remoteVersion;
  }

  private String findRemoteVersion(String body) {
    Document doc = Jsoup.parse(body);
    Elements links = doc.select("a[href]");
    String magic = cm.getAsString(ConfigurationKey.FORMS_UPDATE_URL_MAGIC, "1drv.ms");
    for (Element e : links) {
      String link = e.attr("href");
      if (link.contains(magic)) {
        return e.text().trim();
      }
    }
    return null;
  }

  private String findUpdateURL(String body) {
    Document doc = Jsoup.parse(body);
    Elements links = doc.select("a[href]");
    String magic = cm.getAsString(ConfigurationKey.FORMS_UPDATE_URL_MAGIC, "1drv.ms");
    for (Element e : links) {
      String link = e.attr("href");
      if (link.contains(magic)) {
        return link;
      }
    }
    return null;
  }

  /**
   * update forms, if update available
   *
   * @return 0 if nothing to update, 1 if updated
   */
  @SuppressWarnings("resource")
  public void updateForms() {
    if (formsAlreadyUpdated) {
      logger.info("forms already updated during startup");
    }

    logger.info("Checking for latest Winlink forms");
    var currentLongVersion = getLongVersion();
    logger.info("Currently installed version: " + currentLongVersion);

    String remoteLongVersion = getRemoteLongVersion();
    logger.info("Latest available version: " + remoteLongVersion);

    if (remoteLongVersion.equals(currentLongVersion)) {
      logger.info("The installed version is the same as the most current version");
    }

    boolean okToContinue = Utils
        .promptForBoolean("Update currently installed forms with version " + remoteLongVersion + "? Default [no]: ");
    if (!okToContinue) {
      logger.info("skipping forms update");
      return;
    }

    try {
      HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();

      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(updateURL)).build();

      HttpResponse<String> firstResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseUriString = firstResponse.uri().toURL().toString();
      // https://stackoverflow.com/questions/26541705/how-to-download-a-file-from-onedrive-after-using-the-onedrive-picker-to-get-the
      responseUriString = responseUriString.replace("/redir", "/download");
      logger.debug("updated responseUriString: " + responseUriString);

      request = HttpRequest.newBuilder().uri(URI.create(responseUriString)).build();

      HttpResponse<byte[]> secondResponse = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      byte[] bytes = secondResponse.body();

      if (bytes != null && bytes.length > 1_000_000) {
        logger.info("downloaded new forms: " + bytes.length + " bytes");

        // rename existing forms directory
        String formsDirName = cm.getAsString(ConfigurationKey.FORMS_PATH);
        File formsDir = new File(formsDirName);
        String formsParentDirName = formsDir.getParent();
        File renameDir = new File(formsDir + "-" + getShortVersion());
        logger.info("rename formsDir from: " + formsDirName + " to: " + renameDir.getName());
        formsDir.renameTo(renameDir);

        // write the bytes to a file
        Path zipPath = Path.of(formsParentDirName, "StandardForms.zip");
        File zipFile = zipPath.toFile();
        FileOutputStream fos = new FileOutputStream(zipFile);
        fos.write(bytes);
        fos.close();

        // unzip
        new ZipFile(zipFile).extractAll(formsDirName);

        // rename zip
        Path versionPath = Paths.get(formsDirName, "Standard_Forms_Version.dat");
        longVersion = Files.readString(versionPath);
        File renameZipFile = new File(formsDirName + "-" + getLongVersion() + ".zip");
        zipFile.renameTo(renameZipFile);
        logger.info("wrote zipped forms file to: " + renameZipFile.getName());
        logger.info("downloaded new forms, version: " + getLongVersion());
        formsAlreadyUpdated = true;
      }
    } catch (Exception e) {
      logger.error("Error in updateForms(): " + e.getMessage(), e);
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

  public String getLongVersion() {
    return longVersion;
  }

  public String getShortVersion() {
    return shortVersion;
  }

  public String getUpdateURL() {
    return updateURL;
  }
}
