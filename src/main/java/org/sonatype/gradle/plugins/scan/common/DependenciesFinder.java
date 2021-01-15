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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.scan.module.model.Artifact;
import com.sonatype.insight.scan.module.model.Module;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;

public class DependenciesFinder
{
  private static final String RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME = "_releaseCompile";

  private static final String RELEASE_COMPILE_CONFIGURATION_NAME = "releaseCompileClasspath";

  private static final Set<String> CONFIGURATION_NAMES =
      new HashSet<>(Arrays.asList(
          COMPILE_CLASSPATH_CONFIGURATION_NAME,
          RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME,
          RELEASE_COMPILE_CONFIGURATION_NAME));

  private static final String COPY_CONFIGURATION_NAME = "sonatypeCopyConfiguration";

  private static final String ATTRIBUTES_SUPPORTED_GRADLE_VERSION = "4.0";

  public Set<ResolvedDependency> findResolvedDependencies(Project rootProject, boolean allConfigurations) {
    return new LinkedHashSet<>(rootProject.getAllprojects()).stream()
        .flatMap(project -> project.getConfigurations().stream())
        .filter(configuration -> isAcceptableConfiguration(configuration, allConfigurations))
        .flatMap(configuration -> getDependencies(rootProject, configuration,
            resolvedConfiguration -> resolvedConfiguration.getFirstLevelModuleDependencies().stream()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public Set<ResolvedArtifact> findResolvedArtifacts(Project project, boolean allConfigurations) {
    return new LinkedHashSet<>(project.getConfigurations()).stream()
        .filter(configuration -> isAcceptableConfiguration(configuration, allConfigurations))
        .flatMap(configuration -> getDependencies(project, configuration,
            resolvedConfiguration -> resolvedConfiguration.getResolvedArtifacts().stream()))
        .collect(Collectors.toSet());
  }

  public List<Module> findModules(Project rootProject, boolean allConfigurations) {
    List<Module> modules = new ArrayList<>();

    rootProject.allprojects(project -> {
      Module module = buildModule(project);

      findResolvedArtifacts(project, allConfigurations).forEach(resolvedArtifact -> {
        Artifact artifact = new Artifact().setId(resolvedArtifact.getId().getComponentIdentifier().getDisplayName())
            .setPathname(resolvedArtifact.getFile()).setMonitored(true);
        module.addConsumedArtifact(artifact);
      });

      modules.add(module);
    });

    return modules;
  }

  @VisibleForTesting
  Module buildModule(Project project) {
    Module module = new Module().setIdKind("gradle").setPathname(project.getProjectDir());

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

  @VisibleForTesting
  Configuration createCopyConfiguration(Project project) {
    String configurationName = COPY_CONFIGURATION_NAME;
    for (int i = 0; project.getConfigurations().findByName(configurationName) != null; i++) {
      configurationName += i;
    }
    Configuration copyConfiguration = project.getConfigurations().create(configurationName);
    if (isGradleVersionSupportedForAttributes(project.getGradle().getGradleVersion())) {
      copyConfiguration.attributes(attributeContainer -> {
        ObjectFactory factory = project.getObjects();
        attributeContainer.attribute(Usage.USAGE_ATTRIBUTE, factory.named(Usage.class, Usage.JAVA_RUNTIME));
      });
    }
    return copyConfiguration;
  }

  @VisibleForTesting
  boolean isGradleVersionSupportedForAttributes(String gradleVersion) {
    return gradleVersion.compareTo(ATTRIBUTES_SUPPORTED_GRADLE_VERSION) >= 0;
  }

  @VisibleForTesting
  <T> Stream<T> getDependencies(
      Project project,
      Configuration originalConfiguration,
      Function<ResolvedConfiguration, Stream<T>> function)
  {
    try {
      return function.apply(originalConfiguration.getResolvedConfiguration());
    }
    catch (ResolveException e) {
      Configuration copyConfiguration = createCopyConfiguration(project);

      originalConfiguration.getAllDependencies().all(dependency -> {
        if (dependency instanceof ProjectDependency) {
          Project dependencyProject = ((ProjectDependency) dependency).getDependencyProject();
          project.evaluationDependsOn(dependencyProject.getPath());
        }
        else {
          copyConfiguration.getDependencies().add(dependency);
        }
      });

      return function.apply(copyConfiguration.getResolvedConfiguration());
    }
  }

  private boolean isAcceptableConfiguration(Configuration configuration, boolean allConfigurations) {
    if (configuration.getName().endsWith(COPY_CONFIGURATION_NAME))
      return false;
    if (allConfigurations) {
      return configuration.isCanBeResolved();
    }
    return configuration.isCanBeResolved() && (CONFIGURATION_NAMES.contains(configuration.getName())
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_COMPILE_CONFIGURATION_NAME));
  }
}
