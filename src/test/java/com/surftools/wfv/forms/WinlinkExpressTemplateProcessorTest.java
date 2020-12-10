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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WinlinkExpressTemplateProcessorTest {
  private static final Logger logger = LoggerFactory.getLogger(WinlinkExpressTemplateProcessorTest.class);

  @Test
  public void test_noVars() {
    logger.debug("begin test_noVars");

    final var processor = new WinlinkExpressTemplateProcessor();
    final var source = "hello, world";
    final var expected = source;
    final var variableMap = new HashMap<String, String>();

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    logger.debug("end test_noVars");
  }

  @Test
  public void test_simple() {
    logger.debug("begin test_simple");

    final var processor = new WinlinkExpressTemplateProcessor();
    final var source = "{var greet}, world";
    final var expected = "hello, world";
    final var variableMap = new HashMap<String, String>();
    variableMap.put("greet", "hello");

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    logger.debug("end test_simple");
  }

  @Test
  public void test_multiple() {
    logger.debug("begin test_multiple");

    final var processor = new WinlinkExpressTemplateProcessor();
    final var source = "{var greet} {var greet} {var greet}, world";
    final var expected = "hello hello hello, world";
    final var variableMap = new HashMap<String, String>();
    variableMap.put("greet", "hello");

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    logger.debug("end test_multiple");
  }

  @Test
  public void test_quotes() {
    logger.debug("begin test_quotes");

    final var processor = new WinlinkExpressTemplateProcessor();
    final var source = "{var greet}, \"world\"";
    final var expected = "&quot;hello&quot;, \"world\"";
    final var variableMap = new HashMap<String, String>();
    variableMap.put("greet", "\"hello\"");

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    logger.debug("end test_quotes");
  }

  @Test
  public void test_ampersand() {
    logger.debug("begin test_ampersand");

    final var processor = new WinlinkExpressTemplateProcessor();
    final var source = "{var greet}, &world&";
    final var expected = "&hello&, &world&";
    final var variableMap = new HashMap<String, String>();
    variableMap.put("greet", "&hello&");

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    logger.debug("end test_ampersand");
  }

  @Test
  public void test_notFound_replaceEmptyTrue() {
    logger.debug("begin test_notFound_replaceEmptyTrue");

    final var processor = new WinlinkExpressTemplateProcessor();
    assertTrue(processor.isDoReplaceNotFoundWithEmptyString());

    final var source = "{var greet}, world";
    final var expected = ", world";
    final var variableMap = new HashMap<String, String>();

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    processor.setDoReplaceNotFoundWithEmptyString(false);
    assertFalse(processor.isDoReplaceNotFoundWithEmptyString());

    logger.debug("end test_notFound_replaceEmptyTrue");
  }

  @Test
  public void test_notFound_replaceEmptyFalse() {
    logger.debug("begin test_notFound_replaceEmptyFalse");

    final var processor = new WinlinkExpressTemplateProcessor();
    processor.setDoReplaceNotFoundWithEmptyString(false);
    assertFalse(processor.isDoReplaceNotFoundWithEmptyString());

    final var source = "{var greet}, world";
    final var expected = source;
    final var variableMap = new HashMap<String, String>();

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    logger.debug("end test_notFound_replaceEmptyFalse");
  }

  @Test
  public void test_MiXeD_CaSe_VaR() {
    logger.debug("begin test_MiXeD_CaSe_VaR");

    final var processor = new WinlinkExpressTemplateProcessor();
    final var source = "{VaR greet}, world";
    final var expected = "hello, world";
    final var variableMap = new HashMap<String, String>();
    variableMap.put("greet", "hello");

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    logger.debug("end test_MiXeD_CaSe_VaR");
  }

  @Test
  public void test_MiXeD_CaSe_VaLuE() {
    logger.debug("begin test_MiXeD_CaSe_VaLuE");

    final var processor = new WinlinkExpressTemplateProcessor();
    final var source = "{var GrEeT}, world";
    final var expected = ", world";
    final var variableMap = new HashMap<String, String>();
    variableMap.put("gReEt", "hello");

    final var actual = processor.process(source, variableMap);
    assertNotNull(actual);
    assertEquals(expected, actual);

    logger.debug("end test_MiXeD_CaSe_VaLuE");
  }

}
