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
 * start as web server,
 *
 * accept a Winlink Express (WE) view file, merge with the referenced form, return merged form
 *
 * @author bobt
 *
 */

public class FormViewer {
  private static final Logger logger = LoggerFactory.getLogger(FormViewer.class);

  // create a separate logger so that we can log server access to a file, etc.
  private static final Logger serverLogger = LoggerFactory.getLogger("serverLogger");

  private static final String FV_VERSION = "0.6.1";

  private static final String FILE_UPLOAD_PATH = "/uploadFile";
  private static final String XHR_UPLOAD_PATH = "/uploadXHR";

  private static final String DEFAULT_CONFIG_FILE_NAME = "fv.conf";

  private static IConfigurationManager cm;

  @Option(name = "--config-file", metaVar = "CONFIGURATION_FILE_NAME", usage = "path to configuration file, default: "
      + DEFAULT_CONFIG_FILE_NAME, required = false)
  private String configFileName = DEFAULT_CONFIG_FILE_NAME;

  @Option(name = "--server", usage = "run as http server, default: false", required = false)
  private boolean isServer = false;

  @Option(name = "--updateForms", usage = "update forms, if available and exit, default: false", required = false)
  private boolean updateForms = false;

  public static void main(String[] args) {
    FormViewer app = new FormViewer();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  private void run() {
    logger.info("Winlink Form Viewer (fv), version: " + FV_VERSION + ", processId: " + ProcessHandle.current().pid());
    try {
      cm = new PropertyFileConfigurationManager(configFileName);

      if (updateForms) {
        FormUtils formUtils = new FormUtils(cm);
        formUtils.updateForms();
        logger.info("exiting");
        System.exit(0);
      }

      if (isServer) {
        final int port = cm.getAsInt(ConfigurationKey.SERVER_PORT, 6676);
        if (!Utils.isPortAvailable(port)) {
          Utils.fatal(cm, ConfigurationKey.EMSG_PORT_IN_USE, String.valueOf(port));
        }
        final String ipAddress = Utils.getLocalIPv4Address();
        final String serverUrl = "http://" + ipAddress + ":" + port;
        logger.info("listening on port: " + serverUrl);

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

        return;
      }

      System.err.println(getUsageText());
      System.exit(1);

    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
    }

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
    String errorMessage = null;
    WinlinkExpressViewerParser parser = null;
    String viewContentLowerCase = viewContent.toLowerCase();
    if (viewContentLowerCase.startsWith("<?xml version=\"1.0\"?>")
        && viewContentLowerCase.contains("<rms_express_form>")) {
      parser = new WinlinkExpressViewerParser();
      errorMessage = parser.parse(viewContent, true);
    } else {
      errorMessage = "uploaded content doesn't appear to be valid Winlink View file";
    }

    if (errorMessage != null) {
      ConfigurationKey key = ConfigurationKey.EMSG_CANT_PARSE_VIEW_FILE;
      String defaultValue = key.getErrorMessage();
      String template = cm.getAsString(key, defaultValue);
      errorMessage = String.format(template, errorMessage);
      logger.warn(errorMessage);
      return new FormResults(null, errorMessage, 401);
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

      String initialContent = Files.readString(Paths.get(initialHtmlFileName));
      initialContent = initialContent.replace("$VERSION", FV_VERSION);

      return initialContent;
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
          response.status(401);
          return cm.getAsString(ConfigurationKey.EMSG_NO_UPLOAD_FILE);
        }

        InputStream is = file.getInputStream();
        viewContent = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
      } else if (requestPath.equals(XHR_UPLOAD_PATH)) {
        viewContent = request.body();
      }

      if (viewContent == null || viewContent.length() == 0) {
        Utils.warn(cm, ConfigurationKey.EMSG_NO_UPLOAD_FILE, null);
        response.status(401);
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

  private String getUsageText() {
    String helpText = null;
    String usageFileName = null;
    try {
      usageFileName = cm.getAsString(ConfigurationKey.USAGE_FILE);
      Path usageFilePath = Paths.get(usageFileName);
      helpText = Files.readString(usageFilePath);

      helpText = helpText.replace("$VERSION", FV_VERSION);
    } catch (Exception e) {
      logger.error("Error getting usage file:" + usageFileName + ", " + e.getLocalizedMessage());
    }

    return helpText;
  }
}
