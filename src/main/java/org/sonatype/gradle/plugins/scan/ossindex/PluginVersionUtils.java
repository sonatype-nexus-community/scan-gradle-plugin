/*
 * Copyright (c) 2020-present Sonatype, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sonatype.ossindex.service.client.internal.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PluginVersionUtils
{
  private static final Logger log = LoggerFactory.getLogger(PluginVersionUtils.class);

  private static final String PROPERTIES_PATH = "com/sonatype/insight/client.properties";

  private PluginVersionUtils() {
    // Utils class
  }

  public static String getPluginVersion() {
    String pluginVersion = Version.UNKNOWN;
    try (InputStream stream = PluginVersionUtils.class.getClassLoader().getResourceAsStream(PROPERTIES_PATH)) {
      Properties properties = new Properties();
      properties.load(stream);
      pluginVersion = properties.getProperty("version", Version.UNKNOWN);
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return pluginVersion;
  }
}
