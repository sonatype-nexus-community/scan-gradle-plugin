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

import org.sonatype.ossindex.service.client.transport.UserAgentSupplier;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.util.GradleVersion;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TransportBuilderTest
{
  private TransportBuilder builder;

  @Before
  public void setup() {
    builder = new TransportBuilder();
  }

  @Test
  public void testBuild() {
    Project project = ProjectBuilder.builder().build();
    assertThat(builder.build(project)).isNotNull();
  }

  @Test
  public void testBuildUserAgentSupplier() throws IOException {
    Project project = ProjectBuilder.builder().build();

    UserAgentSupplier result = builder.buildUserAgentSupplier(project);

    assertThat(result).isNotNull();

    String gradleVersion = GradleVersion.current().getVersion();
    String pluginVersion;

    try (InputStream stream = getClass().getResourceAsStream("/com/sonatype/insight/client.properties")) {
      Properties properties = new Properties();
      properties.load(stream);
      pluginVersion = properties.getProperty("version");
    }

    assertThat(result.get()).endsWith("Gradle/" + gradleVersion + " Gradle-Plugin/" + pluginVersion);
  }
}
