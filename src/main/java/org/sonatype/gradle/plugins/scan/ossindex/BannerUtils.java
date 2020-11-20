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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BannerUtils
{
  private static final Logger log = LoggerFactory.getLogger(BannerUtils.class);

  private static final String HEADER_PATH = "org/sonatype/gradle/plugins/scan/ossindex/banner.txt";

  private static final String PROPERTIES_PATH = "com/sonatype/insight/client.properties";

  private BannerUtils() {
    // Utils class
  }

  public static String createBanner() {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(BannerUtils.class.getClassLoader().getResourceAsStream(HEADER_PATH)))) {

      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append(System.lineSeparator());
      }

      sb.append("Gradle Scan version: ")
          .append(PluginVersionUtils.getPluginVersion())
          .append(System.lineSeparator());

      sb.append(StringUtils.repeat("-", 150))
          .append(System.lineSeparator());
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return sb.toString();
  }
}
