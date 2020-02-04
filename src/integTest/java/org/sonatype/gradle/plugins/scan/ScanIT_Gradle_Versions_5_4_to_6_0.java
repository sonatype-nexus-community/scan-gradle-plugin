package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_5_4_to_6_0
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("5.4.1", "5.6.4", "6.0.1");
  }

  public ScanIT_Gradle_Versions_5_4_to_6_0(final String gradleVersion) {
    super(gradleVersion);
  }
}
