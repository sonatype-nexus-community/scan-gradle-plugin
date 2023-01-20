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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.scan.module.model.Artifact;
import com.sonatype.insight.scan.module.model.Dependency;
import com.sonatype.insight.scan.module.model.Module;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.DefaultResolvedDependency;
import org.gradle.api.internal.artifacts.ResolvedConfigurationIdentifier;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class DependenciesFinderTest
{
  private static final String COMMONS_COLLECTIONS_DEPENDENCY = "commons-collections:commons-collections:3.1";

  private DependenciesFinder finder;

  @Before
  public void setup() {
    finder = new DependenciesFinder();
  }

  @Test
  public void testFindResolvedDependencies_includeCompileDependencies() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeRuntimeDependencies() {
    Project project = buildProject(RUNTIME_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyCompileAndroidDependencies() {
    Project project = buildProject("_releaseCompile", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyRuntimeApkAndroidDependencies() {
    Project project = buildProject("_releaseApk", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyRuntimeLibraryAndroidDependencies() {
    Project project = buildProject("_releasePublish", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeCompileAndroidDependencies() {
    Project project = buildProject("releaseCompileClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeRuntimeAndroidDependencies() {
    Project project = buildProject("releaseRuntimeClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyCompileApkAndroidDependenciesUsingVariant() {
    Project project = buildProject("variantProd_ReleaseCompile", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyRuntimeApkAndroidDependenciesUsingVariant() {
    Project project = buildProject("variantProd_ReleaseApk", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyRuntimeLibraryAndroidDependenciesUsingVariant() {
    Project project = buildProject("variantProd_ReleasePublish", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeAndroidCompileDependenciesUsingVariant() {
    Project project = buildProject("variantProdReleaseCompileClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeAndroidRuntimeDependenciesUsingVariant() {
    Project project = buildProject("variantProdReleaseRuntimeClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_omitTestDependencies() {
    Project project = buildProject(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap());
    assertThat(result).isEmpty();
  }

  @Test
  public void testFindResolvedDependencies_includeTestDependencies() {
    Project project = buildProject(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, true, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_multiModuleProject() {
    Project parentProject = ProjectBuilder.builder().withName("parent").build();
    Project childProject = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false, parentProject);

    assertThat(finder.findResolvedDependencies(childProject, false, emptyMap())).hasSize(1);
    assertThat(finder.findResolvedDependencies(parentProject, false, emptyMap())).isEmpty();
  }

  @Test
  public void testFindResolvedDependencies_copyConfigurationAfterResolveException() {
    Project project = spy(buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false));
    ConfigurationContainer configurationContainer = spy(project.getConfigurations());
    Configuration configuration = spy(configurationContainer.iterator().next());

    when(configuration.getResolvedConfiguration()).thenThrow(ResolveException.class);
    when(configurationContainer.stream()).thenReturn(Stream.of(configuration));
    when(project.getConfigurations()).thenReturn(configurationContainer);
    when(project.getAllprojects()).thenReturn(Sets.newHashSet(project));

    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, true, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeCompileDependencies() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeRuntimeDependencies() {
    Project project = buildProject(RUNTIME_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeAndroidCompileDependencies() {
    Project project = buildProject("releaseCompileClasspath", true);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeAndroidRuntimeDependencies() {
    Project project = buildProject("releaseRuntimeClasspath", true);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_omitTestDependencies() {
    Project project = buildProject(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptyMap());
    assertThat(result).isEmpty();
  }

  @Test
  public void testFindResolvedArtifacts_includeTestDependencies() {
    Project project = buildProject(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, true, emptyMap());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testBuildModule_withBasicInfo() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getIdKind()).isEqualTo("gradle");
    assertThat(module.getBuilderInfo(Module.BI_CLM_TOOL)).isEqualTo("gradle");
    assertThat(module.getBuilderInfo(Module.BI_CLM_VERSION)).isEqualTo(PluginVersionUtils.getPluginVersion());
    assertThat(module.getPathname()).isEqualTo(project.getProjectDir().getAbsolutePath());
    assertThat(module.getId()).isEqualTo(project.getName());
  }

  @Test
  public void testBuildModule_withGroup() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    project.setGroup("groupTest");
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getId()).isEqualTo(project.getGroup() + ":" + project.getName());
  }

  @Test
  public void testBuildModule_withGroupAndVersion() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    project.setGroup("groupTest");
    project.setVersion("0.0.1");
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getId()).isEqualTo(project.getGroup() + ":" + project.getName() + ":" + project.getVersion());
  }

  @Test
  public void testFindModules_singleModule() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    List<Module> modules = finder.findModules(project, false, emptySet(), emptyMap());

    assertThat(modules).hasSize(1);

    Module module = modules.get(0);
    assertThat(module.getId()).isEqualTo(project.getName());
    assertThat(module.getConsumedArtifacts()).hasSize(1);
    assertThat(module.getDependencies()).hasSize(1);

    Dependency dependency = module.getDependencies().get(0);
    assertThat(dependency.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
    assertThat(dependency.isDirect()).isTrue();

    Artifact artifact = module.getConsumedArtifacts().get(0);
    assertThat(artifact.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testFindModules_multiModule() {
    Project parentProject = ProjectBuilder.builder().withName("parent").build();
    Project childProject = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false, parentProject);
    List<Module> modules = finder.findModules(parentProject, false, emptySet(), emptyMap());

    assertThat(modules).hasSize(2);

    Module parentModule = modules.get(0);
    assertThat(parentModule.getId()).isEqualTo(parentProject.getName());
    assertThat(parentModule.getConsumedArtifacts()).isEmpty();
    assertThat(parentModule.getParentId()).isNull();

    Module childModule = modules.get(1);
    assertThat(childModule.getId()).isEqualTo(parentProject.getName() + ":" + childProject.getName());
    assertThat(childModule.getConsumedArtifacts()).hasSize(1);
    assertThat(childModule.getDependencies()).hasSize(1);
    assertThat(childModule.getParentId()).isEqualTo(parentProject.getName());

    Dependency dependency = childModule.getDependencies().get(0);
    assertThat(dependency.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
    assertThat(dependency.isDirect()).isTrue();

    Artifact artifact = childModule.getConsumedArtifacts().get(0);
    assertThat(artifact.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testFindModules_multiModuleWithModuleExcluded() {
    Project parentProject = ProjectBuilder.builder().withName("parent").build();
    Project childProject = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false, parentProject);
    List<Module> modules =
        finder.findModules(parentProject, false, Collections.singleton(childProject.getName()), emptyMap());

    assertThat(modules).hasSize(1);
    assertThat(modules.get(0).getId()).isEqualTo(parentProject.getName());
  }

  @Test
  public void testCreateCopyConfiguration() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Configuration configuration = finder.createCopyConfiguration(project, emptyMap());

    assertThat(configuration).isNotNull();
    assertThat(configuration.getName()).isEqualTo("sonatypeCopyConfiguration");

    Usage expectedUsage = project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME);
    assertThat(configuration.getAttributes().getAttribute(Usage.USAGE_ATTRIBUTE)).isEqualTo(expectedUsage);
    assertThat(configuration.getAttributes()
        .getAttribute(Attribute.of(DependenciesFinder.BUILD_TYPE_ATTR_NAME, String.class))).isNull();
  }

  @Test
  public void testCreateCopyConfiguration_WithVariantAttributes() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Map<String, String> variantAttributes = ImmutableMap.of("attribute1", "1", "attribute2", "2");

    Configuration configuration = finder.createCopyConfiguration(project, variantAttributes);

    assertThat(configuration).isNotNull();
    AttributeContainer attributeContainer = configuration.getAttributes().getAttributes();

    assertThat(attributeContainer.getAttribute(Attribute.of("attribute1", String.class))).isEqualTo("1");
    assertThat(attributeContainer.getAttribute(Attribute.of("attribute2", String.class))).isEqualTo("2");
  }

  @Test
  public void testCreateCopyConfiguration_AndroidProject() {
    PluginContainer pluginContainer = mock(PluginContainer.class);
    when(pluginContainer.hasPlugin("com.android.application")).thenReturn(true);

    Project project = spy(buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false));
    when(project.getPlugins()).thenReturn(pluginContainer);

    Configuration configuration = finder.createCopyConfiguration(project, emptyMap());

    assertThat(configuration).isNotNull();
    assertThat(configuration.getName()).isEqualTo("sonatypeCopyConfiguration");

    Usage expectedUsage = project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME);
    assertThat(configuration.getAttributes().getAttribute(Usage.USAGE_ATTRIBUTE)).isEqualTo(expectedUsage);
    assertThat(
        configuration.getAttributes().getAttribute(Attribute.of(DependenciesFinder.BUILD_TYPE_ATTR_NAME, String.class)))
            .isEqualTo("release");
  }

  @Test
  public void testIsGradleVersionSupportedForAttributes() {
    assertThat(finder.isGradleVersionSupportedForAttributes("3.5")).isFalse();
    assertThat(finder.isGradleVersionSupportedForAttributes("3.5.1")).isFalse();
    assertThat(finder.isGradleVersionSupportedForAttributes("4.0")).isTrue();
    assertThat(finder.isGradleVersionSupportedForAttributes("4.0.1")).isTrue();
    assertThat(finder.isGradleVersionSupportedForAttributes("4.0.2")).isTrue();
    assertThat(finder.isGradleVersionSupportedForAttributes("4.1")).isTrue();
  }

  @Test
  public void testGetDependencies_ModuleDependencies() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Configuration originalConfiguration = project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME);

    Stream<ResolvedDependency> dependencies = finder.getDependencies(project, originalConfiguration, emptyMap(),
        resolvedConfiguration -> resolvedConfiguration.getFirstLevelModuleDependencies().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedDependency> list = dependencies.collect(Collectors.toList());
    assertThat(list).hasSize(1);

    Set<ResolvedArtifact> artifacts = list.get(0).getAllModuleArtifacts();
    assertThat(artifacts).hasSize(1);
    assertThat(artifacts.iterator().next().getId().getComponentIdentifier().toString())
        .isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testGetDependencies_ModuleDependencies_WithError() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);

    Configuration originalConfiguration = mock(Configuration.class);
    when(originalConfiguration.getResolvedConfiguration()).thenThrow(ResolveException.class);
    when(originalConfiguration.getAllDependencies())
        .thenReturn(project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME).getAllDependencies());

    Stream<ResolvedDependency> dependencies = finder.getDependencies(project, originalConfiguration, emptyMap(),
        resolvedConfiguration -> resolvedConfiguration.getFirstLevelModuleDependencies().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedDependency> list = dependencies.collect(Collectors.toList());
    assertThat(list).hasSize(1);

    Set<ResolvedArtifact> artifacts = list.get(0).getAllModuleArtifacts();
    assertThat(artifacts).hasSize(1);
    assertThat(artifacts.iterator().next().getId().getComponentIdentifier().toString())
        .isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testGetDependencies_ResolvedArtifacts() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    Configuration originalConfiguration = project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME);

    Stream<ResolvedArtifact> dependencies = finder.getDependencies(project, originalConfiguration, emptyMap(),
        resolvedConfiguration -> resolvedConfiguration.getResolvedArtifacts().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedArtifact> list = dependencies.collect(Collectors.toList());

    assertThat(list).hasSize(1);
    assertThat(list.get(0).getId().getComponentIdentifier().toString()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testGetDependencies_ResolvedArtifacts_WithError() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);

    Configuration originalConfiguration = mock(Configuration.class);
    when(originalConfiguration.getResolvedConfiguration()).thenThrow(ResolveException.class);
    when(originalConfiguration.getAllDependencies())
        .thenReturn(project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME).getAllDependencies());

    Stream<ResolvedArtifact> dependencies = finder.getDependencies(project, originalConfiguration, emptyMap(),
        resolvedConfiguration -> resolvedConfiguration.getResolvedArtifacts().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedArtifact> list = dependencies.collect(Collectors.toList());

    assertThat(list).hasSize(1);
    assertThat(list.get(0).getId().getComponentIdentifier().toString()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testGetDependencies_ResolvedArtifacts_Skip_Unresolvable_Dependencies() {
    Project project = buildProject(COMPILE_CLASSPATH_CONFIGURATION_NAME, false);
    DependencyHandler dependencyHandler = project.getDependencies();
    org.gradle.api.artifacts.Dependency testDependency =
        dependencyHandler.add(COMPILE_CLASSPATH_CONFIGURATION_NAME, "test_group-test_artifact:0.0.1");

    Configuration originalConfiguration = mock(Configuration.class);
    when(originalConfiguration.getResolvedConfiguration()).thenThrow(ResolveException.class);
    when(originalConfiguration.files(testDependency)).thenThrow(ResolveException.class);
    when(originalConfiguration.getAllDependencies())
        .thenReturn(project.getConfigurations().getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME).getAllDependencies());

    Stream<ResolvedArtifact> dependencies = finder.getDependencies(project, originalConfiguration, emptyMap(),
        resolvedConfiguration -> resolvedConfiguration.getResolvedArtifacts().stream());

    assertThat(dependencies).isNotNull();

    List<ResolvedArtifact> list = dependencies.collect(Collectors.toList());

    assertThat(list).hasSize(1);
    assertThat(list.get(0).getId().getComponentIdentifier().toString()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testProcessDependency() {
    testProcessDependency(false);
  }

  @Test
  public void testProcessDependency_avoidCircularDependenciesStackOverflowError() {
    testProcessDependency(true);
  }

  private void testProcessDependency(boolean setupCircularDependencies) {
    ModuleVersionIdentifier parentModuleVersionIdentifier = DefaultModuleVersionIdentifier.newId("g", "a", "v");
    ResolvedConfigurationIdentifier parentResolvedConfigurationIdentifier = new ResolvedConfigurationIdentifier(
        parentModuleVersionIdentifier, "");
    DefaultResolvedDependency parentDependency = new DefaultResolvedDependency(
        parentResolvedConfigurationIdentifier, null);

    ModuleVersionIdentifier singleChildModuleVersionIdentifier = DefaultModuleVersionIdentifier.newId(
        "g2", "a2", "v2");
    ResolvedConfigurationIdentifier singleChildResolvedConfigurationIdentifier = new ResolvedConfigurationIdentifier(
        singleChildModuleVersionIdentifier, "");
    DefaultResolvedDependency singleChildDependency = new DefaultResolvedDependency(
        singleChildResolvedConfigurationIdentifier, null);

    ModuleVersionIdentifier multiChildModuleVersionIdentifier = DefaultModuleVersionIdentifier.newId("g3", "a3", "v3");
    ResolvedConfigurationIdentifier multiChildResolvedConfigurationIdentifier = new ResolvedConfigurationIdentifier(
        multiChildModuleVersionIdentifier, "");
    DefaultResolvedDependency multiChildDependency = new DefaultResolvedDependency(
        multiChildResolvedConfigurationIdentifier, null);

    ModuleVersionIdentifier subChildModuleVersionIdentifier = DefaultModuleVersionIdentifier.newId("g4", "a4", "v4");
    ResolvedConfigurationIdentifier subChildResolvedConfigurationIdentifier = new ResolvedConfigurationIdentifier(
        subChildModuleVersionIdentifier, "");
    DefaultResolvedDependency subChildDependency = new DefaultResolvedDependency(
        subChildResolvedConfigurationIdentifier, null);

    multiChildDependency.addChild(subChildDependency);

    if (setupCircularDependencies) {
      singleChildDependency.addChild(parentDependency);
    }

    parentDependency.addChild(singleChildDependency);
    parentDependency.addChild(multiChildDependency);

    Dependency dependency = finder.processDependency(parentDependency, true, new HashSet<>());
    assertThat(dependency).isNotNull();
    assertThat(dependency.isDirect()).isTrue();
    assertThat(dependency.getId()).isEqualTo("g:a:v");
    assertThat(dependency.getDependencies()).hasSize(2);

    Dependency child1 = dependency.getDependencies().get(0);
    assertThat(child1.isDirect()).isFalse();
    assertThat(child1.getId()).isEqualTo("g2:a2:v2");
    assertThat(child1.getDependencies()).isEmpty();

    Dependency child2 = dependency.getDependencies().get(1);
    assertThat(child2.isDirect()).isFalse();
    assertThat(child2.getId()).isEqualTo("g3:a3:v3");
    assertThat(child2.getDependencies()).hasSize(1);

    Dependency subChild2 = child2.getDependencies().get(0);
    assertThat(subChild2.isDirect()).isFalse();
    assertThat(subChild2.getId()).isEqualTo("g4:a4:v4");
    assertThat(subChild2.getDependencies()).isEmpty();
  }

  private Project buildProject(String configurationName, boolean needToCreateConfiguration) {
    return buildProject(configurationName, needToCreateConfiguration, null);
  }

  private Project buildProject(String configurationName, boolean needToCreateConfiguration, Project parent) {
    ProjectBuilder projectBuilder = ProjectBuilder.builder();
    if (parent != null) {
      projectBuilder.withParent(parent);
    }
    Project project = projectBuilder.build();

    project.getPluginManager().apply("java");
    project.getRepositories().mavenCentral();
    if (needToCreateConfiguration) {
      project.getConfigurations().create(configurationName);
    }
    DependencyHandler dependencyHandler = project.getDependencies();
    dependencyHandler.add(configurationName, COMMONS_COLLECTIONS_DEPENDENCY);
    return project;
  }
}
