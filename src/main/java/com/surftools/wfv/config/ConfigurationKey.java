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

  EMSG_PORT_IN_USE("emsg.port.in.use", "port: %s is already in use. Exiting!"), //
  EMSG_BROWSER_NOT_FOUND("emsg.browser.not.found", "Browser executable: %s not found. Exiting!"), //
  EMSG_URL_FILE_NOT_FOUND("emsg.url.not.found", "URL file: %s not found. Exiting!"), //
  EMSG_INIT_HTML_NOT_FOUND("emsg.initial.html.not.found", "Initial HTML file: %s not found. Exiting!"), //
  EMSG_INBOX_NOT_FOUND("emsg.inbox.not.found", "Inbox directory: %s not found. Exiting!"), //
  EMSG_INBOX_EMPTY("emsg.no.files.in.inbox", "Inbox directory: %s empty. Exiting!"), //
  EMSG_FORMS_DIR_NOT_FOUND("emsg.forms.dir.not.found", "Forms directory: %s not found. Exiting!"), //
  EMSG_FORMS_DIR_NOT_DIR("emsg.forms.dir.not.dir", "Forms directory: %s not a directory. Exiting!"), //
  EMSG_VIEW_FILE_NOT_FOUND("emsg.view.file.not.found", "View File: %s not found. Exiting!"), //
  EMSG_CANT_PARSE_VIEW_FILE("emsg.cant.parse.view.file", "Can't parse view file: %s. Exiting!"), //
  EMSG_NO_FORM_FILE_FOUND("emsg.no.form.file.found", "No form files found matching %s. Exiting!"), //
  EMSG_MULTIPLE_FORM_FILES_FOUND("emsg.multiple.form.files.found", "Multiple form files found matching %s. Exiting!"), //
  EMSG_OUTBOX_NOT_FOUND("emsg.outbox.dir.not.found", "Outbox directory: %s not found. Exiting!"), //
  EMSG_OUTBOX_NOT_DIR("emsg.outbox.dir.not.dir", "Outbox directory: %s not a directory. Exiting!"), //
  EMSG_NO_UPLOAD_FILE("emsg.no.upload.file", "No file fould in upload post. Continuing!"), //

  BROWSER_PATH("browser.path"), //
  FORMS_PATH("forms.path"), //
  INBOX_PATH("inbox.path"), //
  OUTBOX_PATH("outbox.path"), //

  SERVER_INITIAL_HTML("server.initialHtml"), // for internal server
  SERVER_404_HTML("server.404Html"), //
  SERVER_PORT("server.port"), //
  ;

  private final String key;
  private final String errorMessage;

  private ConfigurationKey(String key, String errorMessage) {
    this.key = key;
    this.errorMessage = errorMessage;
  }

  private ConfigurationKey(String key) {
    this(key, null);
  }

  public String getErrorMessage() {
    return errorMessage;
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
