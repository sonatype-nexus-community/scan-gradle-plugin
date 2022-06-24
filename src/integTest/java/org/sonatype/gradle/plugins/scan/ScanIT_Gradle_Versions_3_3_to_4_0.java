package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_3_3_to_4_0
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("3.3", "3.5", "4.0");
  }

  public ScanIT_Gradle_Versions_3_3_to_4_0(final String gradleVersion) {
    super(gradleVersion);
    this.useLegacySyntax = true;
    this.useNewMissingPropertyMessage = false;
  }
}
