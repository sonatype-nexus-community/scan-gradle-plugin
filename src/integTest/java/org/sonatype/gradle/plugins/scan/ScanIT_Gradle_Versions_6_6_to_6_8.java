package org.sonatype.gradle.plugins.scan;

import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

public class ScanIT_Gradle_Versions_6_6_to_6_8
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("6.6", "6.7.1", "6.8.3");
  }

  public ScanIT_Gradle_Versions_6_6_to_6_8(final String gradleVersion) {
    super(gradleVersion);
  }
}
