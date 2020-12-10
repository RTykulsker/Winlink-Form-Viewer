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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wfv.config.ConfigurationKey;
import com.surftools.wfv.config.IConfigurationManager;

public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  public static void openBrowser(IConfigurationManager cm, String url) throws Exception {
    String browserFileName = cm.getAsString(ConfigurationKey.BROWSER_PATH);
    File browserFile = new File(browserFileName);
    if (!browserFile.exists()) {
      Utils.fatal(cm, ConfigurationKey.EMSG_BROWSER_NOT_FOUND, browserFileName);
    }

    ProcessBuilder processBuilder = new ProcessBuilder(browserFileName, url);
    Process process = processBuilder.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    StringBuilder sb = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      sb.append(line);
      sb.append("\n");
    }
    logger.debug(sb.toString());

    int exitCode = process.waitFor();
    logger.debug("browser exited with error code : " + exitCode);

  }

  public static void fatal(IConfigurationManager cm, ConfigurationKey key, String value) {
    String defaultValue = key.getErrorMessage();
    String template = cm.getAsString(key, defaultValue);
    String message = String.format(template, value);
    logger.error(message);
    System.exit(1);
  }

  public static void warn(IConfigurationManager cm, ConfigurationKey key, String value) {
    String defaultValue = key.getErrorMessage();
    String template = cm.getAsString(key, defaultValue);
    String message = String.format(template, value);
    logger.warn(message);
  }

  public static void openBrowser(IConfigurationManager cm, Path url) throws Exception {
    openBrowser(cm, url.toFile().getCanonicalPath());
  }

  public static boolean isPortAvailable(int port) {
    try (Socket ignored = new Socket("localhost", port)) {
      return false;
    } catch (IOException ignored) {
      return true;
    }
  }

  public static String getLocalIPv4Address() {
    String ip = "127.0.0.1";
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        // filters out 127.0.0.1 and inactive interfaces
        if (iface.isLoopback() || !iface.isUp()) {
          continue;
        }

        Enumeration<InetAddress> addresses = iface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress addr = addresses.nextElement();

          if (addr instanceof Inet6Address) {
            continue;
          }

          ip = addr.getHostAddress();
        }
      }
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }
    return ip;
  }

  /**
   * return a String representing a LocalDateTime string
   *
   * @return
   */
  public static String makeTimestamp() {
    final var now = LocalDateTime.now();
    final var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    final var timestamp = now.format(formatter);
    return timestamp;
  }

}
