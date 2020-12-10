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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wfv.config.PropertyFileConfigurationManager;

public class FormUtilsTest {
  private static final Logger logger = LoggerFactory.getLogger(FormUtilsTest.class);
  private static final String DEFAULT_CONFIG_FILE_NAME = "conf/fv.conf";

  @Test
  public void test_version() throws Exception {
    logger.debug("begin test_version");

    final var cm = new PropertyFileConfigurationManager(DEFAULT_CONFIG_FILE_NAME);
    final var fu = new FormUtils(cm);

    assertNotNull(fu);
    logger.debug("version: " + fu.getVersion());

    logger.debug("end test_version");
  }

  /**
   * this is not a unit test
   *
   * @throws Exception
   */
  @Test
  public void test_isFormsUpdateAvailable() throws Exception {
    logger.debug("begin test_isFormsUpdateAvailable");

    final var cm = new PropertyFileConfigurationManager(DEFAULT_CONFIG_FILE_NAME);
    final var fu = new FormUtils(cm);

    boolean isAvailable = fu.isFormsUpdateAvailable();
    if (isAvailable) {
      String updateURL = fu.getUpdateURL();
      assertNotNull(updateURL);
      logger.debug("updateURL: " + updateURL);
    }

    logger.debug("end test_isFormsUpdateAvailable");
  }

  /**
   * this is not a unit test
   *
   * @throws Exception
   */
  @Test
  public void test_updateForms() throws Exception {
    logger.debug("begin test_updateForms");

    final var cm = new PropertyFileConfigurationManager(DEFAULT_CONFIG_FILE_NAME);
    final var fu = new FormUtils(cm);

    fu.updateForms();

    logger.debug("end test_updateForms");
  }
}
