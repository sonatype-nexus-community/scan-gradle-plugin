package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_5_1_to_5_3
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("5.1.1", "5.2.1", "5.3.1");
  }

  public ScanIT_Gradle_Versions_5_1_to_5_3(final String gradleVersion) {
    super(gradleVersion);
  }
}
