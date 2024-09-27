package org.sonatype.gradle.plugins.scan;

import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_6_0_to_6_4
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return List.of("6.0", "6.2.2", "6.4.1");
  }

  public ScanIT_Gradle_Versions_6_0_to_6_4(final String gradleVersion) {
    super(gradleVersion);
  }
}
