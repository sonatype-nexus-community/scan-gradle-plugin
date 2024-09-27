package org.sonatype.gradle.plugins.scan;

import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_5_0_to_5_4
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return List.of("5.0", "5.3.1", "5.4.1");
  }

  public ScanIT_Gradle_Versions_5_0_to_5_4(final String gradleVersion) {
    super(gradleVersion);
  }
}
