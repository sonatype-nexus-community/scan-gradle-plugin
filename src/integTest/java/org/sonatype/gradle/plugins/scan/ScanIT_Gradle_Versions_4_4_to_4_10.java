package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_4_4_to_4_10
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("4.4.1", "4.7", "4.10.3");
  }

  public ScanIT_Gradle_Versions_4_4_to_4_10(final String gradleVersion) {
    super(gradleVersion);
    this.useNewMissingPropertyMessage = false;
  }
}
