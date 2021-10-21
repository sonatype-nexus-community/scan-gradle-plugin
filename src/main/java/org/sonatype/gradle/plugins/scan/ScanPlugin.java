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

import org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqIndexTask;
import org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqPluginIndexExtension;
import org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqPluginScanExtension;
import org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqScanTask;
import org.sonatype.gradle.plugins.scan.ossindex.OssIndexAuditTask;
import org.sonatype.gradle.plugins.scan.ossindex.OssIndexPluginExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ScanPlugin implements Plugin<Project>
{
  private static final String SONATYPE_GROUP = "Sonatype";

  @Override
  public void apply(Project project) {
    project.getExtensions().create("nexusIQScan", NexusIqPluginScanExtension.class, project);
    project.getTasks().register("nexusIQScan", NexusIqScanTask.class, task -> {
      task.setGroup(SONATYPE_GROUP);
      task.setDescription("Scan and evaluate the dependencies of the project using Nexus IQ Server.");
    });

    project.getExtensions().create("nexusIQIndex", NexusIqPluginIndexExtension.class, project);
    project.getTasks().register("nexusIQIndex", NexusIqIndexTask.class, task -> {
    task.setGroup(SONATYPE_GROUP);
    task.setDescription("Saves information about the dependencies of a project into module information "
            + "(module.xml) files that Sonatype CI tools can use to include these dependencies in a scan.");
    });

    project.getExtensions().create("ossIndexAudit", OssIndexPluginExtension.class, project);
    project.getTasks().register("ossIndexAudit", OssIndexAuditTask.class, task -> {
      task.setGroup(SONATYPE_GROUP);
      task.setDescription("Audit the dependencies of the project using OSS Index.");
    });
  }
}
