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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.scan.module.model.Artifact;
import com.sonatype.insight.scan.module.model.Dependency;
import com.sonatype.insight.scan.module.model.Module;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.AttributeMatchingStrategy;
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

public class DependenciesFinder
{
  private final Logger log = LoggerFactory.getLogger(DependenciesFinder.class);

  static final String BUILD_TYPE_ATTR_NAME = "com.android.build.api.attributes.BuildTypeAttr";

  private static final String RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME = "_releaseCompile";

  private static final String RELEASE_RUNTIME_APK_LEGACY_CONFIGURATION_NAME = "_releaseApk";

  private static final String RELEASE_RUNTIME_LIBRARY_LEGACY_CONFIGURATION_NAME = "_releasePublish";

  private static final String RELEASE_COMPILE_CONFIGURATION_NAME = "releaseCompileClasspath";

  private static final String RELEASE_RUNTIME_CONFIGURATION_NAME = "releaseRuntimeClasspath";

  private static final Set<String> CONFIGURATION_NAMES = ImmutableSet.of(
      COMPILE_CLASSPATH_CONFIGURATION_NAME,
      RUNTIME_CLASSPATH_CONFIGURATION_NAME,
      RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME,
      RELEASE_RUNTIME_APK_LEGACY_CONFIGURATION_NAME,
      RELEASE_RUNTIME_LIBRARY_LEGACY_CONFIGURATION_NAME,
      RELEASE_COMPILE_CONFIGURATION_NAME, RELEASE_RUNTIME_CONFIGURATION_NAME);

  private static final String COPY_CONFIGURATION_NAME = "sonatypeCopyConfiguration";

  private static final String ATTRIBUTES_SUPPORTED_GRADLE_VERSION = "4.0";

  public Set<ResolvedDependency> findResolvedDependencies(Project project, boolean allConfigurations) {
    return new LinkedHashSet<>(new LinkedHashSet<>(project.getConfigurations()).stream()
        .filter(configuration -> isAcceptableConfiguration(configuration, allConfigurations))
        .flatMap(configuration -> getDependencies(project, configuration,
            resolvedConfiguration -> resolvedConfiguration.getFirstLevelModuleDependencies().stream()))
        .collect(collectResolvedDependencies()).values());
  }

  public Set<ResolvedArtifact> findResolvedArtifacts(Project project, boolean allConfigurations) {
    return new LinkedHashSet<>(project.getConfigurations()).stream()
        .filter(configuration -> isAcceptableConfiguration(configuration, allConfigurations))
        .flatMap(configuration -> getDependencies(project, configuration,
            resolvedConfiguration -> resolvedConfiguration.getResolvedArtifacts().stream()))
        .collect(Collectors.toSet());
  }

  public List<Module> findModules(Project rootProject, boolean allConfigurations, Set<String> modulesExcluded) {
    List<Module> modules = new ArrayList<>();

    rootProject.allprojects(project -> {
      if (!modulesExcluded.contains(project.getName())) {
        Module module = buildModule(project);

        findResolvedArtifacts(project, allConfigurations).forEach(resolvedArtifact -> {
          ModuleVersionIdentifier artifactId = resolvedArtifact.getModuleVersion().getId();

          Artifact artifact = new Artifact()
              .setId(artifactId.getGroup() + ":" + artifactId.getName() + ":" + artifactId.getVersion())
              .setPathname(resolvedArtifact.getFile())
              .setMonitored(true);

          module.addConsumedArtifact(artifact);
        });

        findResolvedDependencies(project, allConfigurations).forEach(resolvedDependency ->
          module.addDependency(processDependency(resolvedDependency, true, new HashSet<>()))
        );

        modules.add(module);
      }
    });

    return modules;
  }

  @VisibleForTesting
  Module buildModule(Project project) {
    Module module = new Module()
        .setIdKind("gradle")
        .setPathname(project.getProjectDir())
        .setBuilderInfo(Module.BI_CLM_TOOL, "gradle")
        .setBuilderInfo(Module.BI_CLM_VERSION, PluginVersionUtils.getPluginVersion());

    module.setId(getId(project));
    if (project.getParent() != null) {
      module.setParentId(getId(project.getParent()));
    }

    return module;
  }

  String getId(Project project) {
    StringBuilder idBuilder = new StringBuilder();
    if (StringUtils.isNotBlank(project.getGroup().toString())) {
      idBuilder.append(project.getGroup().toString()).append(":");
    }
    idBuilder.append(project.getName());
    if (StringUtils.isNotBlank(project.getVersion().toString())
        && !"unspecified".equals(project.getVersion().toString())) {
      idBuilder.append(":").append(project.getVersion().toString());
    }
    return idBuilder.toString();
  }

  @VisibleForTesting
  Configuration createCopyConfiguration(Project project) {
    String configurationName = COPY_CONFIGURATION_NAME;
    for (int i = 0; project.getConfigurations().findByName(configurationName) != null; i++) {
      configurationName += i;
    }
    Configuration copyConfiguration = project.getConfigurations().create(configurationName);

    if (isGradleVersionSupportedForAttributes(project.getGradle().getGradleVersion())) {
      boolean isAndroidProject = isAndroidProject(project);

      if (isAndroidProject) {
        AttributeMatchingStrategy<String> artifactTypeMatchingStrategy =
            project.getDependencies().getAttributesSchema().attribute(Attribute.of("artifactType", String.class));
        artifactTypeMatchingStrategy.getDisambiguationRules().add(AndroidAttributeDisambiguationRule.class);
      }

      copyConfiguration.attributes(attributeContainer -> {
        ObjectFactory factory = project.getObjects();
        attributeContainer.attribute(Usage.USAGE_ATTRIBUTE, factory.named(Usage.class, Usage.JAVA_RUNTIME));

        if (isAndroidProject) {
          addReleaseBuildTypeAttribute(project, attributeContainer);
        }
      });
    }
    return copyConfiguration;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void addReleaseBuildTypeAttribute(Project project, AttributeContainer attributeContainer) {
    if (GradleVersion.current().compareTo(GradleVersion.version("6.0.1")) >= 0) {
      attributeContainer.attribute(Attribute.of(BUILD_TYPE_ATTR_NAME, String.class), "release");
    }
    else {
      Set<String> configurationNames = new HashSet<String>(CONFIGURATION_NAMES);
      configurationNames.add("fullReleaseRuntimeElements");

      for (String configurationName : configurationNames) {
        Configuration configuration = project.getConfigurations().findByName(configurationName);
        Attribute attributeFound = null;
        Object attributeValue = null;

        if (configuration != null) {
          for (Attribute attribute : configuration.getAttributes().keySet()) {
            if (BUILD_TYPE_ATTR_NAME.equals(attribute.getName())) {
              attributeFound = attribute;
              attributeValue = configuration.getAttributes().getAttribute(attributeFound);
              break;
            }
          }
        }

        if (attributeFound != null) {
          attributeContainer.attribute(Attribute.of(BUILD_TYPE_ATTR_NAME, attributeFound.getType()), attributeValue);
          break;
        }
      }
    }
  }

  @VisibleForTesting
  boolean isGradleVersionSupportedForAttributes(String gradleVersion) {
    return gradleVersion.compareTo(ATTRIBUTES_SUPPORTED_GRADLE_VERSION) >= 0;
  }

  @VisibleForTesting
  boolean isAndroidProject(Project project) {
    PluginContainer pluginContainer = project.getPlugins();
    return pluginContainer.hasPlugin("com.android.application") || pluginContainer.hasPlugin("com.android.library");
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
      Configuration copyConfiguration = copyDependencies(project, originalConfiguration, false);

      try {
        return function.apply(copyConfiguration.getResolvedConfiguration());
      }
      catch (ResolveException lastResortException) {
        copyConfiguration = copyDependencies(project, originalConfiguration, true);
        return function.apply(copyConfiguration.getResolvedConfiguration());
      }
    }
  }

  private Configuration copyDependencies(
      Project project,
      Configuration originalConfiguration,
      boolean skipUnresolvableDependencies)
  {
    Configuration copyConfiguration = createCopyConfiguration(project);

    originalConfiguration.getAllDependencies().all(dependency -> {
      copyConfiguration.getDependencies().add(dependency);

      if (skipUnresolvableDependencies && !(dependency instanceof ProjectDependency)) {
        try {
          originalConfiguration.files(dependency);
        }
        catch (Exception e) {
          log.warn("Unable to process the dependency {}:{}:{} in project {} and configuration {}",
              dependency.getGroup(), dependency.getName(), dependency.getVersion(), project.getName(),
              originalConfiguration.getName());
          copyConfiguration.getDependencies().remove(dependency);
        }
      }
    });

    return copyConfiguration;
  }

  @VisibleForTesting
  Dependency processDependency(
      ResolvedDependency resolvedDependency,
      boolean isDirect,
      Set<String> processedDependencies)
  {
    Dependency dependency = new Dependency()
        .setId(resolvedDependency.getName())
        .setDirect(isDirect);

    processedDependencies.add(resolvedDependency.getName());

    resolvedDependency.getChildren().forEach(child -> {
      if (processedDependencies.add(child.getName())) {
        dependency.addDependency(processDependency(child, false, processedDependencies));
      }
      else if (!isParent(resolvedDependency, child, new HashSet<>())) {
        Dependency childDependency = new Dependency()
            .setId(child.getName())
            .setDirect(false);

        dependency.addDependency(childDependency);
      }
    });

    return dependency;
  }

  private boolean isParent(
      ResolvedDependency dependency,
      ResolvedDependency possibleParent,
      Set<String> processedParents)
  {
    Set<ResolvedDependency> parents = dependency.getParents();
    for (ResolvedDependency parent : parents) {
      if (parent.getName().equals(possibleParent.getName())) {
        return true;
      }
      if (processedParents.add(parent.getName())) {
        isParent(parent, possibleParent, processedParents);
      }
    }

    return false;
  }

  private boolean isAcceptableConfiguration(Configuration configuration, boolean allConfigurations) {
    if (configuration.getName().endsWith(COPY_CONFIGURATION_NAME))
      return false;
    if (allConfigurations) {
      return configuration.isCanBeResolved();
    }
    return configuration.isCanBeResolved() && (CONFIGURATION_NAMES.contains(configuration.getName())
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_RUNTIME_APK_LEGACY_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_RUNTIME_LIBRARY_LEGACY_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_COMPILE_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_RUNTIME_CONFIGURATION_NAME));
  }

  private Collector<ResolvedDependency, ?, LinkedHashMap<String, ResolvedDependency>> collectResolvedDependencies() {
    return Collectors.toMap(ResolvedDependency::getName, Function.identity(), (existing, replacement) -> {
      if (StringUtils.containsAny(replacement.getConfiguration().toLowerCase(Locale.ROOT), "runtime", "release")) {
        return replacement;
      }
      return existing;
    }, LinkedHashMap::new);
  }
}
