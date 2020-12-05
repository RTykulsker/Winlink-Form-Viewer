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

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WinlinkExpressViewerParser {
  private static final Logger logger = LoggerFactory.getLogger(WinlinkExpressTemplateProcessor.class);

  private Map<String, String> keyValueMap;

  public WinlinkExpressViewerParser() {
    keyValueMap = new HashMap<>();
  }

  public String parse(String xmlString, boolean doVerbose) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      ByteArrayInputStream input = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
      Document document = builder.parse(input);
      document.getDocumentElement().normalize();

      NodeList formParametersNodeList = document.getElementsByTagName("form_parameters").item(0).getChildNodes();
      for (int i = 0; i < formParametersNodeList.getLength(); ++i) {
        Node node = formParametersNodeList.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) node;
          String name = element.getNodeName().toLowerCase();
          Node child = element.getFirstChild();
          String value = null;
          if (child != null) {
            value = element.getFirstChild().getNodeValue();
          }
          if (doVerbose) {
            logger.debug("form_parameter, name: " + name + ", value: " + value);
          }
          keyValueMap.put(name, value);
        }
      }

      NodeList variablesList = document.getElementsByTagName("variables").item(0).getChildNodes();
      for (int i = 0; i < variablesList.getLength(); ++i) {
        Node node = variablesList.item(i);

        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) node;
          String name = element.getNodeName().toLowerCase();
          Node child = element.getFirstChild();
          if (child == null) {
            continue;
          }
          String value = element.getFirstChild().getNodeValue();
          if (doVerbose) {
            logger.debug("variables, name: " + name + ", value: " + value);
          }
          keyValueMap.put(name, value);
        }
      }
      logger.debug("after parsing, map has " + keyValueMap.size() + " entries");
    } catch (Exception e) {
      return e.getMessage();
    }
    return null;
  }

  public String getValue(String key) {
    return keyValueMap.get(key);
  }

  public Map<String, String> getVariableMap() {
    return keyValueMap;
  }
}
