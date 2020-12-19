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

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wfv.config.ConfigurationKey;
import com.surftools.wfv.config.IConfigurationManager;

public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

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
    final LocalDateTime now = LocalDateTime.now();
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    final String timestamp = now.format(formatter);
    return timestamp;
  }

  /**
   * Converts a String to a boolean
   *
   * @param str
   *          -- the String to check
   * @return -- true or false
   */
  public static boolean toBoolean(final String str) {
    if (str == null || str.length() == 0) {
      return false;
    }

    if (str == "true") {
      return true;
    }

    switch (str.length()) {
    case 1: {
      final char ch0 = str.charAt(0);
      if (ch0 == 'y' || ch0 == 'Y' || ch0 == 't' || ch0 == 'T' || ch0 == '1') {
        return true;
      }
      if (ch0 == 'n' || ch0 == 'N' || ch0 == 'f' || ch0 == 'F' || ch0 == '0') {
        return false;
      }
      break;
    }

    case 2: {
      final char ch0 = str.charAt(0);
      final char ch1 = str.charAt(1);
      if ((ch0 == 'o' || ch0 == 'O') && (ch1 == 'n' || ch1 == 'N')) {
        return true;
      }
      if ((ch0 == 'n' || ch0 == 'N') && (ch1 == 'o' || ch1 == 'O')) {
        return false;
      }
      break;
    }

    case 3: {
      final char ch0 = str.charAt(0);
      final char ch1 = str.charAt(1);
      final char ch2 = str.charAt(2);
      if ((ch0 == 'y' || ch0 == 'Y') && (ch1 == 'e' || ch1 == 'E') && (ch2 == 's' || ch2 == 'S')) {
        return true;
      }
      if ((ch0 == 'o' || ch0 == 'O') && (ch1 == 'f' || ch1 == 'F') && (ch2 == 'f' || ch2 == 'F')) {
        return false;
      }
      break;
    }

    case 4: {
      final char ch0 = str.charAt(0);
      final char ch1 = str.charAt(1);
      final char ch2 = str.charAt(2);
      final char ch3 = str.charAt(3);
      if ((ch0 == 't' || ch0 == 'T') && (ch1 == 'r' || ch1 == 'R') && (ch2 == 'u' || ch2 == 'U')
          && (ch3 == 'e' || ch3 == 'E')) {
        return true;
      }
      break;
    }

    case 5: {
      final char ch0 = str.charAt(0);
      final char ch1 = str.charAt(1);
      final char ch2 = str.charAt(2);
      final char ch3 = str.charAt(3);
      final char ch4 = str.charAt(4);
      if ((ch0 == 'f' || ch0 == 'F') && (ch1 == 'a' || ch1 == 'A') && (ch2 == 'l' || ch2 == 'L')
          && (ch3 == 's' || ch3 == 'S') && (ch4 == 'e' || ch4 == 'E')) {
        return false;
      }
      break;
    }

    default:
      break;
    }

    return false;
  }

}
