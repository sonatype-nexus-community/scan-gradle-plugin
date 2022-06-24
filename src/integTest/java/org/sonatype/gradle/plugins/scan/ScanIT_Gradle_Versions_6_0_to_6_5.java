package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_6_0_to_6_5
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("6.0", "6.2.2", "6.5.1");
  }

  public ScanIT_Gradle_Versions_6_0_to_6_5(final String gradleVersion) {
    super(gradleVersion);
    this.useNewMissingPropertyMessage = false;
  }
}
