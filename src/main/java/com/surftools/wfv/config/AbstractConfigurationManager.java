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

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractConfigurationManager implements IConfigurationManager {
  protected Map<ConfigurationKey, String> map;

  public AbstractConfigurationManager() {
    map = new HashMap<>();
  }

  @Override
  public String get(ConfigurationKey key) {
    return getAsString(key);
  }

  @Override
  public String get(ConfigurationKey key, String defaultValue) {
    return getAsString(key, defaultValue);
  }

  @Override
  public String getAsString(ConfigurationKey key) {
    return getAsString(key, null);
  }

  @Override
  public String getAsString(ConfigurationKey key, String defaultValue) {
    String value = map.get(key);
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  @Override
  public int getAsInt(ConfigurationKey key) {
    return getAsInt(key, null);
  }

  @Override
  public int getAsInt(ConfigurationKey key, Integer defaultValue) {
    String stringValue = map.get(key);
    if (stringValue == null) {
      stringValue = defaultValue.toString();
    }

    return Integer.valueOf(stringValue);
  }

}
