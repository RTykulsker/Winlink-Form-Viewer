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

public class FormUtils {
  private static final Logger logger = LoggerFactory.getLogger(FormUtils.class);

  private final IConfigurationManager cm;
  private final File formsDir;
  private final String version;

  public FormUtils(IConfigurationManager cm) throws Exception {
    String formsDirName = cm.getAsString(ConfigurationKey.FORMS_PATH);
    formsDir = new File(formsDirName);
    if (!formsDir.exists()) {
      Utils.fatal(cm, ConfigurationKey.EMSG_FORMS_DIR_NOT_FOUND, formsDirName);
    }
    if (!formsDir.isDirectory()) {
      Utils.fatal(cm, ConfigurationKey.EMSG_FORMS_DIR_NOT_DIR, formsDirName);
    }
    this.cm = cm;

    Path versionPath = Paths.get(formsDirName, "Standard_Forms_Version.dat");
    version = Files.readString(versionPath);

    logger.debug("Forms path: " + formsDir + ", version: " + version);
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

  public String getVersion() {
    return version;
  }
}
