/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.wfv.config;

public enum ConfigurationKey {
  MYCALL("mycall"), //
  MAILBOX_PATH("mailbox.path"), //
  PAT_PATH("pat.path"), //

  BROWSER_PATH("browser.path"), //
  FORMS_PATH("forms.path"), //
  INBOX_PATH("inbox.path"), //
  OUTBOX_PATH("outbox.path"), //

  SERVER_INITIAL_HTML("server.initialHtml"), // for internal server
  SERVER_404_HTML("server.404Html"), //
  SERVER_PORT("server.port"), //
  ;

  private final String key;

  private ConfigurationKey(String key) {
    this.key = key;
  }

  public static ConfigurationKey fromString(String string) {
    for (ConfigurationKey key : ConfigurationKey.values()) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return key;
  }
}
