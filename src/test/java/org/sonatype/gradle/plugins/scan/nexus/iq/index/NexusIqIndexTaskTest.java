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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import hidden.com.sonatype.insight.scan.module.model.Module;
import hidden.com.sonatype.insight.scan.module.model.io.ModuleIoManager;

import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;

import com.google.common.collect.Sets;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqIndexTask.MODULE_XML_FILE;
import static org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqPluginScanExtension.SONATYPE_CLM_FOLDER;

@RunWith(MockitoJUnitRunner.class)
public class NexusIqIndexTaskTest
{
  @Mock
  private DependenciesFinder dependenciesFinderMock;

  @Mock
  private ModuleIoManager moduleIoManagerMock;

  @Test
  public void testSaveModule_singleModule() throws IOException {
    Module module = new Module()
        .setId("test-module")
        .setPathname("test-module");
    File file = Paths.get("test-module", "build", SONATYPE_CLM_FOLDER, MODULE_XML_FILE).toFile();

    when(dependenciesFinderMock.findModules(any(Project.class), eq(false), anySet()))
        .thenReturn(Collections.singletonList(module));
    doNothing().when(moduleIoManagerMock).writeModule(file, module);

    NexusIqIndexTask task = buildIndexTask(Collections.emptySet());
    task.setDependenciesFinder(dependenciesFinderMock);
    task.setModuleIoManager(moduleIoManagerMock);
    task.saveModule();

    verify(dependenciesFinderMock).findModules(any(Project.class), eq(false), anySet());
    verify(moduleIoManagerMock).writeModule(file, module);
  }

  @Test
  public void testSaveModule_multipleModules() throws IOException {
    Module module1 = new Module()
        .setId("test-module-1")
        .setPathname("test-module-1");
    Module module2 = new Module()
        .setId("test-module-2")
        .setPathname("test-module-2");
    File file1 = Paths.get("test-module-1", "build", SONATYPE_CLM_FOLDER, MODULE_XML_FILE).toFile();
    File file2 = Paths.get("test-module-2", "build", SONATYPE_CLM_FOLDER, MODULE_XML_FILE).toFile();

    when(dependenciesFinderMock.findModules(any(Project.class), eq(false), anySet()))
        .thenReturn(Arrays.asList(module1, module2));
    doNothing().when(moduleIoManagerMock).writeModule(file1, module1);
    doNothing().when(moduleIoManagerMock).writeModule(file2, module2);

    NexusIqIndexTask task = buildIndexTask(Collections.emptySet());
    task.setDependenciesFinder(dependenciesFinderMock);
    task.setModuleIoManager(moduleIoManagerMock);
    task.saveModule();

    verify(dependenciesFinderMock).findModules(any(Project.class), eq(false), anySet());
    verify(moduleIoManagerMock).writeModule(file1, module1);
    verify(moduleIoManagerMock).writeModule(file2, module2);
  }

  @Test
  public void testSaveModule_excludeModules() throws IOException {
    Set<String> modulesExcluded = Sets.newHashSet("test-module-1", "test-module-2");

    when(dependenciesFinderMock.findModules(any(Project.class), eq(false), eq(modulesExcluded)))
        .thenReturn(Collections.emptyList());

    NexusIqIndexTask task = buildIndexTask(modulesExcluded);
    task.setDependenciesFinder(dependenciesFinderMock);
    task.saveModule();

    verify(dependenciesFinderMock).findModules(any(Project.class), eq(false), eq(modulesExcluded));
  }

  private NexusIqIndexTask buildIndexTask(Set<String> modulesExcluded) {
    Project project = ProjectBuilder.builder().build();
    NexusIqPluginIndexExtension extension = new NexusIqPluginIndexExtension(project);
    extension.setModulesExcluded(modulesExcluded);
    project.getExtensions().add("nexusIQIndex", extension);
    return project.getTasks().create("nexusIQIndex", NexusIqIndexTask.class);
  }
}
