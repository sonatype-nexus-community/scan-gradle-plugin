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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

@RunWith(Parameterized.class)
public class ScanPluginIntegrationTest
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("4.2.1", "4.3.1", "4.4.1", "4.5.1", "4.6", "4.7", "4.8.1", "4.9", "4.10.3", "5.1.1", "5.2.1",
        "5.3.1", "5.4.1", "5.6.4", "6.0.1");
  }

  private String gradleVersion;

  public ScanPluginIntegrationTest(String gradleVersion) {
    this.gradleVersion = gradleVersion;
  }

  @Rule
  public final TemporaryFolder testProjectDir = new TemporaryFolder();
  private File buildFile;

  @Before
  public void setup() throws IOException {
    buildFile = testProjectDir.newFile("build.gradle");
  }

  @Test
  public void testScanTask_MissingTaskConfiguration_NexusIQ() throws IOException {
    writeFile(buildFile, "missing-scan.gradle");

    assertThatThrownBy(() -> GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQ").build())
        .hasMessageContaining("No value has been specified for property 'applicationId")
        .hasMessageContaining("No value has been specified for property 'username'")
        .hasMessageContaining("No value has been specified for property 'password'")
        .hasMessageContaining("No value has been specified for property 'serverUrl'");
  }

  @Test
  public void testScanTask_NoPolicyAction_NexusIQ() throws IOException {
    writeFile(buildFile, "control.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQScan", "--info").build();

    assertBuildOutputText_NexusIQ(result, "None");

    assertThat(result.task(":nexusIQScan").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testScanTask_WarnPolicyAction_NexusIQ() throws IOException {
    writeFile(buildFile, "policy-warn.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQScan", "--info").build();

    assertBuildOutputText_NexusIQ(result, "Warn");

    assertThat(result.task(":nexusIQScan").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testScanTask_FailPolicyAction_NexusIQ() throws IOException {
    writeFile(buildFile, "policy-fail.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQScan", "--info").buildAndFail();

    assertBuildOutputText_NexusIQ(result, "Failure");
    assertThat(result.task(":nexusIQScan").getOutcome()).isEqualTo(FAILED);
  }

  private void writeFile(File destination, String resourceName) throws IOException {
    InputStream contentStream = getClass().getClassLoader().getResourceAsStream(resourceName);

    BufferedWriter output = null;
    try {
      output = new BufferedWriter(new FileWriter(destination));
      IOUtils.copy(contentStream, output, StandardCharsets.UTF_8);
    }
    finally {
      if (output != null) {
        output.close();
      }
    }
  }

  private void assertBuildOutputText_NexusIQ(BuildResult result, String policyAction) {
    assertThat(result.getOutput()).contains("Policy Action: " + policyAction);
    assertThat(result.getOutput()).contains("Number of components affected: 0 critical, 0 severe, 0 moderate");
    assertThat(result.getOutput()).contains("Number of grandfathered policy violations: 0");
    assertThat(result.getOutput()).contains("The detailed report can be viewed online at simulated/report");
  }
}
