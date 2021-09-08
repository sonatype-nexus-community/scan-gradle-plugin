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
package org.sonatype.gradle.plugins.scan.nexus.iq.index;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.scan.module.model.Module;
import com.sonatype.insight.scan.module.model.io.ModuleIoManager;

import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusIqIndexTask
    extends DefaultTask
{
  private final Logger log = LoggerFactory.getLogger(NexusIqIndexTask.class);

  private final NexusIqPluginIndexExtension extension;

  private DependenciesFinder dependenciesFinder;

  private ModuleIoManager moduleIoManager;

  public NexusIqIndexTask() {
    extension = getProject().getExtensions().getByType(NexusIqPluginIndexExtension.class);
    dependenciesFinder = new DependenciesFinder();
    moduleIoManager = new ModuleIoManager(log);
  }

  @TaskAction
  public void saveModule() {
    try {
      List<Module> modules =
          dependenciesFinder.findModules(getProject(), extension.isAllConfigurations(), extension.getModulesExcluded());
      File file = new File(extension.getModuleFile());
      List<File> files = new ArrayList<>(modules.size());

      if (modules.size() == 1) {
        moduleIoManager.writeModule(file, modules.get(0));
        files.add(file);
      }
      else {
        File parent = new File(extension.getModuleFile()).getParentFile();
        String filename = Files.getNameWithoutExtension(file.getName());
        for (Module module : modules) {
          File moduleFile = new File(parent, filename + "-" + module.getId().replace(':', '-') + ".xml");
          moduleIoManager.writeModule(moduleFile, module);
          files.add(moduleFile);
        }
      }

      log.info("Saved module information to {}", StringUtils.join(files, ", "));
    }
    catch (Exception e) {
      throw new GradleException("Could not save the module descriptor for the project: " + e.getMessage(), e);
    }
  }

  @VisibleForTesting
  void setDependenciesFinder(DependenciesFinder dependenciesFinder) {
    this.dependenciesFinder = dependenciesFinder;
  }

  @VisibleForTesting
  void setModuleIoManager(ModuleIoManager moduleIoManager) {
    this.moduleIoManager = moduleIoManager;
  }

  @Input
  public boolean isAllConfigurations() {
    return extension.isAllConfigurations();
  }

  @Input
  public Set<String> getModulesExcluded() {
    return extension.getModulesExcluded();
  }

  @Input
  public String getModuleFile() {
    return extension.getModuleFile();
  }
}
