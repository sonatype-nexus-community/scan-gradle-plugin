package org.sonatype.gradle.plugins.scan;

import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_5_5_to_5_6
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return List.of("5.5", "5.6", "5.6.4");
  }

  public ScanIT_Gradle_Versions_5_5_to_5_6(final String gradleVersion) {
    super(gradleVersion);
  }
}
