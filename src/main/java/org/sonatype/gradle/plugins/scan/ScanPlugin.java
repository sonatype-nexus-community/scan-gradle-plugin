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

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.util.GradleVersion;

public class ScanPlugin implements Plugin<Project>
{
  private static final boolean IS_GRADLE_MIN_7_4 =
      GradleVersion.current().compareTo(GradleVersion.version("7.4")) >= 0;

  private static final String SONATYPE_GROUP = "Sonatype";

  private static final String TASK_NOT_COMPATIBLE_WITH_CONFIG_CACHE_REASON =
      "Task needs to access the project configuration";

  @Override
  @SuppressWarnings("deprecation")
  public void apply(Project project) {
    project.getExtensions().create("nexusIQScan", NexusIqPluginScanExtension.class, project);
    createTask(project, "nexusIQScan", NexusIqScanTask.class, task -> {
      task.setGroup(SONATYPE_GROUP);
      task.setDescription("Scan and evaluate the dependencies of the project using Nexus IQ Server.");
      if (IS_GRADLE_MIN_7_4) {
        task.notCompatibleWithConfigurationCache(TASK_NOT_COMPATIBLE_WITH_CONFIG_CACHE_REASON);
      }
    });

    project.getExtensions().create("nexusIQIndex", NexusIqPluginIndexExtension.class, project);
    createTask(project, "nexusIQIndex", NexusIqIndexTask.class, task -> {
      task.setGroup(SONATYPE_GROUP);
      task.setDescription("Saves information about the dependencies of a project into module information "
            + "(module.xml) files that Sonatype CI tools can use to include these dependencies in a scan.");
      if (IS_GRADLE_MIN_7_4) {
        task.notCompatibleWithConfigurationCache(TASK_NOT_COMPATIBLE_WITH_CONFIG_CACHE_REASON);
      }
    });

    project.getExtensions().create("ossIndexAudit", OssIndexPluginExtension.class, project);
    createTask(project, "ossIndexAudit", OssIndexAuditTask.class, task -> {
      task.setGroup(SONATYPE_GROUP);
      task.setDescription("Audit the dependencies of the project using OSS Index.");
      if (IS_GRADLE_MIN_7_4) {
        task.notCompatibleWithConfigurationCache(TASK_NOT_COMPATIBLE_WITH_CONFIG_CACHE_REASON);
      }
    });
  }

  private static <T extends Task> void createTask(
      Project project,
      String name,
      Class<T> type,
      Action<? super T> configuration)
  {
    project.getTasks().register(name, type, configuration);
  }
}
