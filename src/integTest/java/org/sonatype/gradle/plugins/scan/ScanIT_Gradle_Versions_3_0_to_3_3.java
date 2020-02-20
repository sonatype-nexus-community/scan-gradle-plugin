package org.sonatype.gradle.plugins.scan;

import java.util.Arrays;
import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_3_0_to_3_3
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return Arrays.asList("3.0", "3.2.1", "3.3");
  }

  public ScanIT_Gradle_Versions_3_0_to_3_3(final String gradleVersion) {
    super(gradleVersion);
    this.useLegacySyntax = true;
  }
}
