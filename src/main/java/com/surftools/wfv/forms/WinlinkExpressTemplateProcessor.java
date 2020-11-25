/**

The MIT License (MIT)

Copyright (c) 2018, Robert Tykulsker

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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WinlinkExpressTemplateProcessor {
  private static final Logger logger = LoggerFactory.getLogger(WinlinkExpressTemplateProcessor.class);

  private final static String regex = "\\{var ([^}]++)\\}";
  private final static Pattern pattern = Pattern.compile(regex);

  private boolean doReplaceNotFoundWithEmptyString;

  public WinlinkExpressTemplateProcessor() {
    doReplaceNotFoundWithEmptyString = true;
  }

  public String process(String source, Map<String, String> variableMap) {
    // https://stackoverflow.com/questions/17462146
    Matcher matcher = pattern.matcher(source);
    String result = source;
    while (matcher.find()) {
      String token = matcher.group(); // Ex: {var fizz}
      String tokenKey = matcher.group(1).toLowerCase(); // Ex: fizz

      String replacementValue = variableMap.get(tokenKey);
      if ((replacementValue == null) && (doReplaceNotFoundWithEmptyString)) {
        replacementValue = "";
      }
      if (replacementValue == null) {
        logger.warn("Source string contained an unsupported token: " + tokenKey);
      } else {
        try {
          logger.debug("replacing: " + tokenKey + " with " + replacementValue);
          result = result.replaceAll(Pattern.quote(token), replacementValue);
        } catch (Exception e) {
          logger.error("Exception for token: " + token + ", " + e.getMessage());
        }
      }

      // if (variableMap.containsKey(tokenKey)) {
      // replacementValue = variableMap.get(tokenKey);
      //
      // logger.debug("replacing: " + tokenKey + " with " + replacementValue);
      // try {
      // result = result.replaceAll(Pattern.quote(token), replacementValue);
      // } catch (Exception e) {
      // logger.error("Exception for token: " + token + ", " + e.getMessage());
      // }
      // } else {
      // if (doReplaceNotFoundWithEmptyString) {
      // try {
      // replacementValue = "";
      // logger.debug("replacing: " + tokenKey + " with " + replacementValue);
      // result = result.replaceAll(Pattern.quote(token), "");
      // } catch (Exception e) {
      // logger.error("Exception for token: " + token + ", " + e.getMessage());
      // }
      // } else {
      // logger.warn("Source string contained an unsupported token: " + tokenKey);
      // }
      // }
    }

    return result;
  }

  public boolean isDoReplaceNotFoundWithEmptyString() {
    return doReplaceNotFoundWithEmptyString;
  }

  public void setDoReplaceNotFoundWithEmptyString(boolean doReplaceNotFoundWithEmptyString) {
    this.doReplaceNotFoundWithEmptyString = doReplaceNotFoundWithEmptyString;
  }

}
