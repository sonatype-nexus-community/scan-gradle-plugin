package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_3_4_to_3_5
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("3.4", "3.4.1", "3.5.1");
  }

  public ScanIT_Gradle_Versions_3_4_to_3_5(final String gradleVersion) {
    super(gradleVersion);
  }
}
