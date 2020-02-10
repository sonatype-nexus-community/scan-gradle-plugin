package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_4_5_to_4_7
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("4.5.1", "4.6", "4.7");
  }

  public ScanIT_Gradle_Versions_4_5_to_4_7(final String gradleVersion) {
    super(gradleVersion);
  }
}
