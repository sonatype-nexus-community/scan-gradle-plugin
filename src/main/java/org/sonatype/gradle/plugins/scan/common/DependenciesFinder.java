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
package org.sonatype.gradle.plugins.scan.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.scan.module.model.Artifact;
import com.sonatype.insight.scan.module.model.Module;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;

public class DependenciesFinder
{
  private static final Set<String> CONFIGURATION_NAMES =
      new HashSet<>(Arrays.asList(COMPILE_CLASSPATH_CONFIGURATION_NAME, "releaseCompileClasspath"));

  public Set<ResolvedArtifact> findResolvedArtifacts(Project project) {
    return project.getConfigurations().stream()
        .filter(configuration -> CONFIGURATION_NAMES.contains(configuration.getName()))
        .flatMap(configuration -> configuration.getResolvedConfiguration().getResolvedArtifacts().stream())
        .collect(Collectors.toSet());
  }

  public List<Module> findModules(Project rootProject) {
    List<Module> modules = new ArrayList<>();

    rootProject.allprojects(project -> {
      Module module = buildModule(project);

      findResolvedArtifacts(project).forEach(resolvedArtifact -> {
        Artifact artifact = new Artifact()
            .setId(resolvedArtifact.getId().getComponentIdentifier().getDisplayName())
            .setPathname(resolvedArtifact.getFile())
            .setMonitored(true);
        module.addConsumedArtifact(artifact);
      });

      modules.add(module);
    });

    return modules;
  }

  @VisibleForTesting
  Module buildModule(Project project) {
    Module module = new Module()
        .setIdKind("gradle")
        .setPathname(project.getProjectDir());

    StringBuilder idBuilder = new StringBuilder();
    if (StringUtils.isNotBlank(project.getGroup().toString())) {
      idBuilder.append(project.getGroup().toString()).append(":");
    }
    idBuilder.append(project.getName());
    if (StringUtils.isNotBlank(project.getVersion().toString())
        && !"unspecified".equals(project.getVersion().toString())) {
      idBuilder.append(":").append(project.getVersion().toString());
    }
    module.setId(idBuilder.toString());

    return module;
  }
}
