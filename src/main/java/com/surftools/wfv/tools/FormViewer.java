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

  private static final String DEFAULT_CONFIG_FILE_NAME = "src/main/resources/fv.conf";

  private IConfigurationManager cm;

  @Option(name = "--config-file", metaVar = "CONFIGURATION_FILE_NAME", usage = "path to configuration file, default: "
      + DEFAULT_CONFIG_FILE_NAME, required = false)
  private String configFileName = DEFAULT_CONFIG_FILE_NAME;

  @Option(name = "--view-file", metaVar = "VIEW_FILE_NAME", usage = "path to viewer file, default: (none)", required = false)
  private String viewFileName = null;

  @Option(name = "--server", metaVar = "SERVER_MODE", usage = "run as http server, default: false", required = false)
  private boolean isServer = false;

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
    try {

      // TODO-- support multiple versions of template files, auto-unzip,
      // TODO-- support fetching latest version of template files from winlink

      cm = new PropertyFileConfigurationManager(configFileName);

      if (isServer && viewFileName != null) {
        throw new RuntimeException("can't run as server AND handle file: " + viewFileName);
      }

      if (isServer) {
        final int port = cm.getAsInt(ConfigurationKey.SERVER_PORT, 8080);
        final var initHandler = new InitHandler();
        final var uploadHandler = new UploadHandler();
        final var notFoundHandler = new NotFoundHandler();
        Spark.port(port);
        Spark.get("/", initHandler);
        Spark.post("/view", uploadHandler);
        Spark.get("*", notFoundHandler);
        Spark.post("*", notFoundHandler);
        Spark.put("*", notFoundHandler);
        Spark.delete("*", notFoundHandler);

        final String ipAddress = Utils.getLocalIPv4Address();
        final String serverUrl = "http://" + ipAddress + ":" + port;
        Utils.openBrowser(cm.getAsString(ConfigurationKey.BROWSER_PATH), serverUrl);

        logger.info("main thread exiting; listening on port: " + serverUrl);
        return;
      }

      // command-line processing
      viewFileName = resolveViewFile(viewFileName, cm);
      String viewContent = Files.readString(Paths.get(viewFileName));
      logger.debug("viewFile: " + viewFileName + ", got " + viewContent.length() + " bytes");

      FormResults results = generateResults(viewContent);

      Path resultPath = Paths.get(cm.getAsString(ConfigurationKey.OUTBOX_PATH), results.displayFormName);
      Files.writeString(resultPath, results.resultString);
      logger.debug("wrote " + results.resultString.length() + " bytes to " + resultPath.toFile().getCanonicalPath());

      Utils.openBrowser(cm.getAsString(ConfigurationKey.BROWSER_PATH), resultPath);

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
      logger.debug("using supplied viewFileName: " + viewFileName);
      return viewFileName;
    }

    File dir = new File(cm.getAsString(ConfigurationKey.INBOX_PATH));
    logger.debug("using inbox: " + dir.getCanonicalPath());
    File[] files = dir.listFiles();
    if (files == null || files.length == 0) {
      throw new RuntimeException("no files in inbox: " + dir.getCanonicalPath());
    }

    File lastModifiedFile = files[0];
    for (int i = 1; i < files.length; i++) {
      if (lastModifiedFile.lastModified() < files[i].lastModified()) {
        lastModifiedFile = files[i];
      }
    }

    logger.debug("returning file: " + lastModifiedFile.getCanonicalPath());
    return lastModifiedFile.getCanonicalPath();
  }

  /**
   * this is the money shot! generate output, given the content of the viewer: find the matching form and substitute
   * values
   *
   * @param viewContent
   * @return
   * @throws Exception
   */
  private FormResults generateResults(String viewContent) throws Exception {
    WinlinkExpressViewerParser parser = new WinlinkExpressViewerParser();
    parser.parse(viewContent, true);

    String displayFormName = parser.getValue("display_form");
    logger.debug("displayFormName: " + displayFormName);

    FormUtils formUtils = new FormUtils(cm.getAsString(ConfigurationKey.FORMS_PATH));
    String formFileName = formUtils.findFormFile(displayFormName);
    String formContent = Files.readString(Paths.get(formFileName));
    logger.debug("formFile: " + formFileName + ", got " + formContent.length() + " bytes");

    var variableMap = parser.getVariableMap();
    WinlinkExpressTemplateProcessor tp = new WinlinkExpressTemplateProcessor();
    String resultString = tp.process(formContent, variableMap);
    return new FormResults(displayFormName, resultString);
  }

  class FormResults {
    final String displayFormName;
    final String resultString;

    public FormResults(String displayFormName, String resultString) {
      this.displayFormName = displayFormName;
      this.resultString = resultString;
    }
  }

  class NotFoundHandler implements Route {

    @Override
    public Object handle(Request request, Response response) throws Exception {
      logger.debug("not found: host: " + request.ip() + ", method: " + request.requestMethod() + ", pathInfo: "
          + request.pathInfo());
      response.status(404);
      String htmlFileName = cm.getAsString(ConfigurationKey.SERVER_404_HTML);
      return Files.readString(Paths.get(htmlFileName));
    }

  }

  class InitHandler implements Route {

    @Override
    public Object handle(Request request, Response response) throws Exception {
      String initialHtmlFileName = cm.getAsString(ConfigurationKey.SERVER_INITIAL_HTML);
      logger.debug("serving initial html from: " + initialHtmlFileName);
      return Files.readString(Paths.get(initialHtmlFileName));
    }

  }

  class UploadHandler implements Route {

    @Override
    public Object handle(Request request, Response response) throws Exception {

      MultipartConfigElement multipartConfigElement = new MultipartConfigElement("");
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
      Part file = request.raw().getPart("file"); // file is name of the upload form
      InputStream is = file.getInputStream();
      String viewContent = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));

      FormResults results = generateResults(viewContent);
      logger.info(commonLogFormat(request, results.displayFormName, results.resultString));
      return results.resultString;
    }

    private String commonLogFormat(Request request, String displayFormName, String resultString) {

      // [10/Oct/2000:13:55:36 -0700]
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss xxxx");
      String timeString = formatter.format(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()));

      StringBuilder sb = new StringBuilder();
      sb.append(request.ip());
      sb.append(" - - [");
      sb.append(timeString);
      sb.append("] ");
      sb.append("\"POST /view/" + displayFormName + "\"");
      sb.append(" 200 ");
      sb.append(resultString.length());
      return sb.toString();

    }
  }
}
