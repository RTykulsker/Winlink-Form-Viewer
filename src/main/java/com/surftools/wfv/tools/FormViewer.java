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

package com.surftools.wfv.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wfv.config.ConfigurationKey;
import com.surftools.wfv.config.IConfigurationManager;
import com.surftools.wfv.config.PropertyFileConfigurationManager;
import com.surftools.wfv.forms.FormUtils;
import com.surftools.wfv.forms.WinlinkExpressTemplateProcessor;
import com.surftools.wfv.forms.WinlinkExpressViewerParser;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

/**
 * view a Winlink Express (WE) form in a browser, with values populated source can be from a web page or cli (last file
 * in inbox, or named file)
 *
 * @author bobt
 *
 */

public class FormViewer {
  private static final Logger logger = LoggerFactory.getLogger(FormViewer.class);

  // create a separate logger so that we can log server access to a file, etc.
  private static final Logger serverLogger = LoggerFactory.getLogger("serverLogger");

  private static final String FV_VERSION = "0.5.2";

  private static final String FILE_UPLOAD_PATH = "/uploadFile";
  private static final String XHR_UPLOAD_PATH = "/uploadXHR";

  private static final String DEFAULT_CONFIG_FILE_NAME = "fv.conf";

  private static IConfigurationManager cm;

  @Option(name = "--config-file", metaVar = "CONFIGURATION_FILE_NAME", usage = "path to configuration file, default: "
      + DEFAULT_CONFIG_FILE_NAME, required = false)
  private String configFileName = DEFAULT_CONFIG_FILE_NAME;

  @Option(name = "--view-file", metaVar = "VIEW_FILE_NAME", usage = "path to viewer file, default: (none)", required = false)
  private String viewFileName = null;

  @Option(name = "--server", usage = "run as http server, default: false", required = false)
  private boolean isServer = false;

  @Option(name = "--updateFormsAndExit", usage = "update forms, if available and exit, default: false", required = false)
  private boolean updateFormsAndExit = false;

  @Option(name = "--help", usage = "show help and exit, default: false", required = false)
  private boolean showHelpAndExit = false;

  public static void main(String[] args) {
    FormViewer app = new FormViewer();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("\n\n" + getHelpText());
    }
  }

  private void run() {
    try {
      cm = new PropertyFileConfigurationManager(configFileName);

      if (showHelpAndExit) {
        System.out.println(getHelpText());
        return;
      }

      if (updateFormsAndExit) {
        FormUtils formUtils = new FormUtils(cm);
        logger.info("Current forms version: " + formUtils.getVersion());
        int retCode = formUtils.updateForms();
        System.exit(retCode);
      }

      if (isServer && viewFileName != null) {
        throw new RuntimeException("can't run as server AND handle file: " + viewFileName);
      }

      if (isServer) {
        final int port = cm.getAsInt(ConfigurationKey.SERVER_PORT, 6676);
        if (!Utils.isPortAvailable(port)) {
          Utils.fatal(cm, ConfigurationKey.EMSG_PORT_IN_USE, String.valueOf(port));
        }
        final String ipAddress = Utils.getLocalIPv4Address();
        final String serverUrl = "http://" + ipAddress + ":" + port;
        final long pid = ProcessHandle.current().pid();
        logger.info("listening on port: " + serverUrl + ", processId: " + pid);

        final Route notFoundHandler = new NotFoundHandler();
        final Route uploadHandler = new UploadHandler();
        Spark.port(port);
        Spark.get("/", new InitHandler());
        Spark.post(FILE_UPLOAD_PATH, uploadHandler);
        Spark.post(XHR_UPLOAD_PATH, uploadHandler);
        Spark.get("*", notFoundHandler);
        Spark.post("*", notFoundHandler);
        Spark.put("*", notFoundHandler);
        Spark.delete("*", notFoundHandler);
        Spark.init();

        Utils.openBrowser(cm, serverUrl);

        return;
      }

      // command-line processing
      viewFileName = resolveViewFile(viewFileName, cm);
      String viewContent = Files.readString(Paths.get(viewFileName));
      logger.debug("viewFile: " + viewFileName + ", got " + viewContent.length() + " bytes");

      FormResults results = generateResults(viewContent);

      String outboxDirName = cm.getAsString(ConfigurationKey.OUTBOX_PATH);

      File outBoxDir = new File(outboxDirName);
      if (!outBoxDir.exists()) {
        Utils.fatal(cm, ConfigurationKey.EMSG_OUTBOX_NOT_FOUND, outboxDirName);
      }
      if (!outBoxDir.isDirectory()) {
        Utils.fatal(cm, ConfigurationKey.EMSG_OUTBOX_NOT_DIR, outboxDirName);
      }

      Path resultPath = Paths.get(outboxDirName, results.displayFormName);
      Files.writeString(resultPath, results.resultString);
      logger.debug("wrote " + results.resultString.length() + " bytes to " + resultPath.toFile().getCanonicalPath());

      Utils.openBrowser(cm, resultPath);

    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
    }

  }

  /**
   * return either explicitly named viewer, or latest file in inbox
   *
   * @param viewFileName
   * @param cm
   * @return
   */
  private String resolveViewFile(String viewFileName, IConfigurationManager cm) throws Exception {
    if (viewFileName != null) {
      File viewFile = new File(viewFileName);
      if (!viewFile.exists()) {
        Utils.fatal(cm, ConfigurationKey.EMSG_VIEW_FILE_NOT_FOUND, viewFileName);
      }
      logger.debug("using supplied viewFileName: " + viewFileName);
      return viewFileName;
    }

    File dir = new File(cm.getAsString(ConfigurationKey.INBOX_PATH));
    String dirName = dir.getCanonicalPath();
    if (!dir.exists()) {
      Utils.fatal(cm, ConfigurationKey.EMSG_INBOX_NOT_FOUND, dirName);
    }

    logger.debug("using inbox: " + dirName);
    File[] files = dir.listFiles();
    if (files == null || files.length == 0) {
      String message = getEmptyInboxMessage(dirName);
      logger.info(message);
      System.exit(0);
    }

    File lastModifiedFile = files[0];
    for (int i = 1; i < files.length; i++) {
      if (lastModifiedFile.lastModified() < files[i].lastModified()) {
        lastModifiedFile = files[i];
      }
    }

    logger.info("using last modified file: " + lastModifiedFile.getName() + ", from inbox: " + dir.getCanonicalPath());
    return lastModifiedFile.getCanonicalPath();
  }

  /**
   * return appropriate text if inbox is empty
   *
   * Since this might be a complex message, we'll first try reading the text from a file
   *
   * If that fails, we'll look for text in a configuration parameter
   *
   * If that fails, we'll take take from configuration key
   *
   * @param dirName
   * @return
   */
  private String getEmptyInboxMessage(String dirName) {
    String template = null;
    String emptyInboxFileName = cm.getAsString(ConfigurationKey.EMPTY_INBOX_FILE);
    try {
      Path emptyInboxPath = Paths.get(emptyInboxFileName);
      template = Files.readString(emptyInboxPath);
    } catch (Exception e) {
      logger.info("couldn't get empty inbox text from file: " + emptyInboxFileName + ", " + e.getLocalizedMessage());
    }

    if (template == null) {
      ConfigurationKey key = ConfigurationKey.EMSG_INBOX_EMPTY;
      template = cm.getAsString(key, key.getErrorMessage());
    }

    String message = template.replace("${inboxDir}", dirName);
    return message;
  }

  /**
   * this is the money shot!
   *
   * generate output, given the content of the viewer: find the matching form and substitute values
   *
   * @param viewContent
   * @return
   * @throws Exception
   */
  private FormResults generateResults(String viewContent) throws Exception {
    WinlinkExpressViewerParser parser = new WinlinkExpressViewerParser();
    String errorMessage = parser.parse(viewContent, true);
    if (errorMessage != null) {
      ConfigurationKey key = ConfigurationKey.EMSG_CANT_PARSE_VIEW_FILE;
      if (isServer) {
        String defaultValue = key.getErrorMessage();
        String template = cm.getAsString(key, defaultValue);
        errorMessage = String.format(template, errorMessage);
        logger.warn(errorMessage);
      } else {
        Utils.fatal(cm, key, errorMessage);
      }
      return new FormResults(null, errorMessage, 500);
    }

    String displayFormName = parser.getValue("display_form");
    logger.debug("displayFormName: " + displayFormName);

    FormUtils formUtils = new FormUtils(cm);
    String formFileName = formUtils.findFormFile(displayFormName);
    String formContent = Files.readString(Paths.get(formFileName));
    logger.debug("formFile: " + formFileName + ", got " + formContent.length() + " bytes");

    Map<String, String> variableMap = parser.getVariableMap();
    WinlinkExpressTemplateProcessor tp = new WinlinkExpressTemplateProcessor();
    String resultString = tp.process(formContent, variableMap);
    return new FormResults(displayFormName, resultString, 200);
  }

  class FormResults {
    final String displayFormName;
    final String resultString;
    final int responseCode;

    public FormResults(String displayFormName, String resultString, int responseCode) {
      this.displayFormName = displayFormName;
      this.resultString = resultString;
      this.responseCode = responseCode;
    }
  }

  class NotFoundHandler implements Route {

    @Override
    public Object handle(Request request, Response response) throws Exception {
      logger.info("not found: host: " + request.ip() + ", method: " + request.requestMethod() + ", pathInfo: "
          + request.pathInfo());
      response.status(404);
      String htmlFileName = cm.getAsString(ConfigurationKey.SERVER_404_HTML);
      File htmlFile = new File(htmlFileName);
      if (!htmlFile.exists()) {
        String message = String.format("404 HTML file: %s not found. Substituting!", htmlFileName);
        logger.warn(message);
        String html = "<!DOCTYPE html><html><title>Not Found</title><body><h1>Not Found</h1></body></html>";
        return html;
      }
      return Files.readString(Paths.get(htmlFileName));
    }

  }

  class InitHandler implements Route {

    @Override
    public Object handle(Request request, Response response) throws Exception {
      String initialHtmlFileName = cm.getAsString(ConfigurationKey.SERVER_INITIAL_HTML);

      File initialHtmlFile = new File(initialHtmlFileName);
      if (!initialHtmlFile.exists()) {
        Utils.fatal(cm, ConfigurationKey.EMSG_INIT_HTML_NOT_FOUND, initialHtmlFileName);
      }

      logger.info("serving initial html from: " + initialHtmlFileName);
      return Files.readString(Paths.get(initialHtmlFileName));
    }

  }

  class UploadHandler implements Route {

    @Override
    public Object handle(Request request, Response response) throws Exception {
      String viewContent = null;
      String requestPath = request.pathInfo();
      if (requestPath.equals(FILE_UPLOAD_PATH)) {
        logger.info("requestPath: " + requestPath);

        // handle form upload; gets placed into a multipart ...
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement("");
        request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
        Part file = request.raw().getPart("file"); // file is name of the upload form

        if (file == null) {
          Utils.warn(cm, ConfigurationKey.EMSG_NO_UPLOAD_FILE, null);
          response.status(500);
          return cm.getAsString(ConfigurationKey.EMSG_NO_UPLOAD_FILE);
        }

        InputStream is = file.getInputStream();
        viewContent = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
      } else if (requestPath.equals(XHR_UPLOAD_PATH)) {
        viewContent = request.body();
      }

      if (viewContent == null || viewContent.length() == 0) {
        Utils.warn(cm, ConfigurationKey.EMSG_NO_UPLOAD_FILE, null);
        response.status(500);
        return cm.getAsString(ConfigurationKey.EMSG_NO_UPLOAD_FILE);
      }

      FormResults results = generateResults(viewContent);
      serverLogger.info(commonLogFormat(request, results.displayFormName, results));
      response.status(results.responseCode);
      return results.resultString;
    }
  }

  private String commonLogFormat(Request request, String displayFormName, FormResults results) {
    // [10/Oct/2000:13:55:36 -0700]
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss xxxx");
    String timeString = formatter.format(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()));

    StringBuilder sb = new StringBuilder();
    sb.append(request.ip());
    sb.append(" - - [");
    sb.append(timeString);
    sb.append("] ");
    sb.append("\"POST" + request.pathInfo() + "/" + displayFormName + "\"");
    sb.append(" " + results.responseCode + " ");
    sb.append(results.resultString.length());
    return sb.toString();
  }

  @Deprecated
  // TODO remove
  private static String getHelpText() {
    final String defaultText = ""; // "
    // This is fv, a Winlink Form Viewer, version $VERSION.
    //
    // FV works in conjunction with with Winlink Express (WE).
    // FV combines the small "view" file transmitted by WE with the the
    // associated form (which is not transmitted). The result is then displayed
    // in a browser. This is very useful when the view file is transmitted by
    // regular (SMTP) email version radio (WE) email
    //
    // FV can operate in several modes, each with a corresponding script in the
    // $FV_HOME/bin directory:
    // -- fv-file: read the view file supplied on the command line
    //
    // -- fv-inbox: read the most most recent view file found in the inbox.
    // The default location for the inbox is $FV_HOME/conf/inbox, but you can
    // override this in the configuration file (see below).
    //
    // -- fv-server: start as a web server that allows you to "upload" or use
    // "drag-and-drop". After displaying a form, you can specify another file
    // by refreshing your browser (typically, F5). The server listens on port
    // 6676 by default, but you can change this via the configuration file
    // (see below).
    //
    // -- fv-update: this attempts to check whether the version of the forms
    // library used is up to date with the version supplied by WE. If not, it
    // attempts to download and install the most up to date version. This is
    // an experimental feature and is likely to break as the Winlink team
    // change how they handle updates. If necessary, you can manually install
    // a new forms library. The location is specified in the configuration
    // file (see below).
    //
    // -- fv-help: print this usage message.
    //
    // FV needs to read a configuration file to properly work. The default
    // configuration file is located in $FV_HOME/conf/fv.conf. You can use an
    // alternative configuration file by specifying with --config-file <name>
    //
    // FV uses a logging configuration file at $FV_HOME/conf/logback.xml to
    // control program logging
    // """;
    if (cm == null) {
      return defaultText;
    }

    String helpText = null;
    String usageFileName = null;
    try {

      if (cm == null) {
        helpText = defaultText;
      } else {
        usageFileName = cm.getAsString(ConfigurationKey.USAGE_FILE);
        Path usageFilePath = Paths.get(usageFileName);
        helpText = Files.readString(usageFilePath);
      }
      helpText = helpText.replace("$VERSION", FV_VERSION);
    } catch (Exception e) {
      logger.debug("Error getting usage file:" + usageFileName + ", " + e.getLocalizedMessage());
    }

    return helpText;
  }
}
