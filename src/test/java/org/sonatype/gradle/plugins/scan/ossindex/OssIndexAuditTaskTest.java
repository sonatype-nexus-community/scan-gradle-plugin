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
package org.sonatype.gradle.plugins.scan.ossindex;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.goodies.packageurl.PackageUrlBuilder;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;
import org.sonatype.ossindex.service.client.OssindexClient;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.DefaultResolvedDependency;
import org.gradle.api.internal.artifacts.ResolvedConfigurationIdentifier;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OssIndexAuditTaskTest
{
  private static final String COMMONS_COLLECTIONS_DEPENDENCY = "commons-collections:commons-collections:3.1";

  private static final PackageUrl COMMONS_COLLECTIONS_PURL = new PackageUrlBuilder()
      .type("maven")
      .namespace("commons-collections")
      .name("commons-collections")
      .version("3.1")
      .build();

  @Mock
  private OssindexClient ossIndexClientMock;

  @Test
  public void testAudit_noVulnerabilities() throws Exception {
    setupComponentReport(false);
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false, null);

    taskSpy.audit();

    verify(ossIndexClientMock).requestComponentReports(eq(Collections.singletonList(COMMONS_COLLECTIONS_PURL)));
  }

  @Test
  public void testAudit_vulnerabilities() throws Exception {
    setupComponentReport(true);
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false, null);

    assertThatThrownBy(taskSpy::audit)
        .isInstanceOf(GradleException.class)
        .hasMessageContaining("Vulnerabilities detected, check log output to review them");

    verify(ossIndexClientMock).requestComponentReports(eq(Collections.singletonList(COMMONS_COLLECTIONS_PURL)));
  }

  @Test
  public void testAudit_vulnerabilitiesNoFailOnDetection() throws Exception {
    setupComponentReport(true);
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false, (project, extension) -> extension.setFailOnDetection(false));

    assertThatCode(taskSpy::audit).doesNotThrowAnyException();

    verify(ossIndexClientMock).requestComponentReports(eq(Collections.singletonList(COMMONS_COLLECTIONS_PURL)));
  }

  @Test
  public void testAudit_verifyModulesIncludedIsApplied() throws Exception {
    setupComponentReport(true);
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false, (project, extension) -> extension.setModulesIncluded(Collections.singleton("does-not-exist")));
    
    taskSpy.audit();
    
    // Note: in the non-mock implementation the audit() method would throw a GradleException with the
    // message "Could not audit the project: One or more coordinates required"
    verify(ossIndexClientMock, never()).requestComponentReports(null);
  }

  @Test
  public void testAudit_verifyModulesExcludedAppliedAfterModulesIncluded() throws Exception {
    setupComponentReport(true);
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false, (project, extension) -> {
      extension.setModulesIncluded(Collections.singleton(project.getName()));
      extension.setModulesExcluded(Collections.singleton(project.getName()));
    });

    taskSpy.audit();

    // Note: in the non-mock implementation the audit() method would throw a GradleException with the
    // message "Could not audit the project: One or more coordinates required"
    verify(ossIndexClientMock, never()).requestComponentReports(null);
  }

  @Test
  public void testAudit_vulnerabilitiesBecauseModuleIncluded() throws Exception {
    setupComponentReport(true);
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false, (project, extension) -> extension.setModulesIncluded(Collections.singleton(project.getName())));

    assertThatThrownBy(taskSpy::audit)
        .isInstanceOf(GradleException.class)
        .hasMessageContaining("Vulnerabilities detected, check log output to review them");

    verify(ossIndexClientMock).requestComponentReports(eq(Collections.singletonList(COMMONS_COLLECTIONS_PURL)));
  }

  @Test
  public void testAudit_novulnerabilitiesBecauseModuleExcluded() throws Exception {
    setupComponentReport(true);
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false, (project, extension) -> extension.setModulesExcluded(Collections.singleton(project.getName())));
    
    taskSpy.audit();
  }

  @Test
  public void testAudit_simulated() throws Exception {
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(true, null);
    taskSpy.audit();
    verify(ossIndexClientMock, never()).requestComponentReports(anyList());
  }

  @Test
  public void testBuildDependenciesMap_avoidCircularDependenciesStackOverflowError() {
    ModuleVersionIdentifier parentModuleVersionIdentifier = DefaultModuleVersionIdentifier.newId("g", "a", "v");
    ResolvedConfigurationIdentifier parentResolvedConfigurationIdentifier =
        new ResolvedConfigurationIdentifier(parentModuleVersionIdentifier, "");
    DefaultResolvedDependency parentDependency =
        new DefaultResolvedDependency(parentResolvedConfigurationIdentifier, null);

    ModuleVersionIdentifier singleChildModuleVersionIdentifier = DefaultModuleVersionIdentifier.newId("g2", "a2", "v2");
    ResolvedConfigurationIdentifier singleChildResolvedConfigurationIdentifier =
        new ResolvedConfigurationIdentifier(singleChildModuleVersionIdentifier, "");
    DefaultResolvedDependency singleChildDependency =
        new DefaultResolvedDependency(singleChildResolvedConfigurationIdentifier, null);

    ModuleVersionIdentifier multiChildModuleVersionIdentifier = DefaultModuleVersionIdentifier.newId("g3", "a3", "v3");
    ResolvedConfigurationIdentifier multiChildResolvedConfigurationIdentifier =
        new ResolvedConfigurationIdentifier(multiChildModuleVersionIdentifier, "");
    DefaultResolvedDependency multiChildDependency =
        new DefaultResolvedDependency(multiChildResolvedConfigurationIdentifier, null);

    ModuleVersionIdentifier subChildModuleVersionIdentifier = DefaultModuleVersionIdentifier.newId("g4", "a4", "v4");
    ResolvedConfigurationIdentifier subChildResolvedConfigurationIdentifier =
        new ResolvedConfigurationIdentifier(subChildModuleVersionIdentifier, "");
    DefaultResolvedDependency subChildDependency =
        new DefaultResolvedDependency(subChildResolvedConfigurationIdentifier, null);

    multiChildDependency.addChild(subChildDependency);

    // circular dependency
    singleChildDependency.addChild(parentDependency);
    parentDependency.addChild(singleChildDependency);
    parentDependency.addChild(multiChildDependency);

    OssIndexAuditTask taskSpy = buildAuditTaskSpy(true, null);
    BiMap<ResolvedDependency, PackageUrl> dependenciesMap = HashBiMap.create();
    taskSpy.buildDependenciesMap(parentDependency, dependenciesMap);

    assertThat(dependenciesMap).containsOnlyKeys(parentDependency, singleChildDependency, multiChildDependency,
        subChildDependency);
  }

  @Test
  public void testBuildResponseHandler_defaultResponseHandler() {
    OssIndexAuditTask taskSpy =
        buildAuditTaskSpy(true, (project, extension) -> extension.setOutputFormat(OutputFormat.DEFAULT));
    assertThat(taskSpy.buildResponseHandler()).isInstanceOf(DefaultResponseHandler.class);
  }

  @Test
  public void testBuildResponseHandler_dependencyGraphResponseHandler() {
    OssIndexAuditTask taskSpy =
        buildAuditTaskSpy(true, (project, extension) -> extension.setOutputFormat(OutputFormat.DEPENDENCY_GRAPH));
    assertThat(taskSpy.buildResponseHandler()).isInstanceOf(DependencyGraphResponseHandler.class);
  }

  @Test
  public void testBuildResponseHandler_cycloneDxResponseHandler() {
    OssIndexAuditTask taskSpy =
        buildAuditTaskSpy(true, (project, extension) -> extension.setOutputFormat(OutputFormat.JSON_CYCLONE_DX_1_4));
    assertThat(taskSpy.buildResponseHandler()).isInstanceOf(CycloneDxResponseHandler.class);
  }

  private OssIndexAuditTask buildAuditTaskSpy(boolean isSimulated, BiConsumer<Project, OssIndexPluginExtension> extensionContributor) {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("java");
    project.getRepositories().mavenCentral();

    DependencyHandler dependencyHandler = project.getDependencies();
    dependencyHandler.add(COMPILE_CLASSPATH_CONFIGURATION_NAME, COMMONS_COLLECTIONS_DEPENDENCY);

    OssIndexPluginExtension extension = new OssIndexPluginExtension(project);
    extension.setSimulationEnabled(isSimulated);
    if (extensionContributor != null) {
      extensionContributor.accept(project, extension);
    }
    project.getExtensions().add("ossIndexAudit", extension);

    OssIndexAuditTask task = spy(project.getTasks().create("ossIndexAudit", OssIndexAuditTask.class));
    doReturn(ossIndexClientMock).when(task).buildOssIndexClient();

    return task;
  }

  private void setupComponentReport(boolean includeVulnerability) throws Exception {
    ComponentReport report = new ComponentReport();
    report.setCoordinates(COMMONS_COLLECTIONS_PURL);

    if (includeVulnerability) {
      ComponentReportVulnerability vulnerability = new ComponentReportVulnerability();
      vulnerability.setTitle("Title 123");
      vulnerability.setCvssScore(4f);
      vulnerability.setReference(new URI("http://test/123"));

      report.setVulnerabilities(Collections.singletonList(vulnerability));
    }

    Map<PackageUrl, ComponentReport> response = ImmutableMap.of(COMMONS_COLLECTIONS_PURL, report);
    when(ossIndexClientMock.requestComponentReports(eq(Collections.singletonList(COMMONS_COLLECTIONS_PURL))))
        .thenReturn(response);
  }
}
