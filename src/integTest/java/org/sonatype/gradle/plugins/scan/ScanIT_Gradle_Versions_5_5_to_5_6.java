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
package org.sonatype.gradle.plugins.scan;

import org.junit.runners.Parameterized;

import java.util.List;

public class ScanIT_Gradle_Versions_5_5_to_5_6
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return List.of("5.5", "5.6", "5.6.4");
  }

  public ScanIT_Gradle_Versions_5_5_to_5_6(final String gradleVersion) {
    super(gradleVersion);
  }
}
