package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_4_0_to_4_1
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("4.0", "4.0.2", "4.1");
  }

  public ScanIT_Gradle_Versions_4_0_to_4_1(final String gradleVersion) {
    super(gradleVersion);
  }
}
