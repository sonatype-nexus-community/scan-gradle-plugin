/*
 * Copyright (c) 2020-present Sonatype, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonatype.gradle.plugins.scan;

import org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqPluginIndexExtension;
import org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqIndexTask;
import org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqPluginScanExtension;
import org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqScanTask;
import org.sonatype.gradle.plugins.scan.ossindex.OssIndexAuditTask;
import org.sonatype.gradle.plugins.scan.ossindex.OssIndexPluginExtension;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanPluginTest
{
  private ScanPlugin plugin;

  @Before
  public void setup() {
    plugin = new ScanPlugin();
  }

  @Test
  public void testApply() {
    Project project = ProjectBuilder.builder().build();
    plugin.apply(project);

    assertThat(project.getTasks().getByName("nexusIQScan")).isInstanceOf(NexusIqScanTask.class);
    assertThat(project.getExtensions().getByName("nexusIQScan")).isInstanceOf(NexusIqPluginScanExtension.class);

    assertThat(project.getTasks().getByName("nexusIQIndex")).isInstanceOf(NexusIqIndexTask.class);
    assertThat(project.getExtensions().getByName("nexusIQIndex")).isInstanceOf(NexusIqPluginIndexExtension.class);

    assertThat(project.getTasks().getByName("ossIndexAudit")).isInstanceOf(OssIndexAuditTask.class);
    assertThat(project.getExtensions().getByName("ossIndexAudit")).isInstanceOf(OssIndexPluginExtension.class);
  }
}
