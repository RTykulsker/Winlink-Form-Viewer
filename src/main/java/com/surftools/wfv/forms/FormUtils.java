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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wfv.tools.FormViewer;

public class FormUtils {
  private static final Logger logger = LoggerFactory.getLogger(FormViewer.class);

  private final File formsDir;
  private final String version;

  public FormUtils(String formsDirName) throws Exception {
    formsDir = new File(formsDirName);
    if (!formsDir.exists()) {
      throw new RuntimeException("Forms path: " + formsDir + " not found");
    }
    if (!formsDir.isDirectory()) {
      throw new RuntimeException("Forms path: " + formsDir + " is not a directory");
    }

    Path versionPath = Paths.get(formsDirName, "Standard_Forms_Version.dat");
    version = Files.readString(versionPath);

    logger.debug("Forms path: " + formsDir + ", version: " + version);
  }

  /**
   * return canonical pathname for form
   *
   * @param displayFormName
   *
   * @param viewFileName
   * @return
   * @throws IOException
   */
  public String findFormFile(String displayFormName) throws Exception {
    List<Path> matchingPaths = new ArrayList<>();
    Files.walk(Paths.get(formsDir.getCanonicalFile().toURI())).filter(Files::isRegularFile).forEach((f) -> {
      String fileName = f.getFileName().toString();
      if (fileName.equals(displayFormName)) {
        if (fileName.endsWith(displayFormName)) {
          matchingPaths.add(f);
          logger.debug("found matching filename: " + fileName);
        }
      }
    });

    if (matchingPaths.size() > 1) {
      throw new RuntimeException("found multiple matching fileNames");
    }

    return matchingPaths.get(0).toFile().getCanonicalPath();
  }

  public String getVersion() {
    return version;
  }
}
