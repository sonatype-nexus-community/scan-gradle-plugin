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
import java.util.Arrays;
import java.util.Map;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.goodies.packageurl.PackageUrlBuilder;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;
import org.sonatype.ossindex.service.client.OssindexClient;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false);

    taskSpy.audit();

    verify(ossIndexClientMock).requestComponentReports(eq(Arrays.asList(COMMONS_COLLECTIONS_PURL)));
  }

  @Test
  public void testAudit_vulnerabilities() throws Exception {
    setupComponentReport(true);
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(false);

    assertThatThrownBy(() -> taskSpy.audit())
        .isInstanceOf(GradleException.class)
        .hasMessageContaining("Vulnerabilities detected, check log output to review them");

    verify(ossIndexClientMock).requestComponentReports(eq(Arrays.asList(COMMONS_COLLECTIONS_PURL)));
  }

  @Test
  public void testAudit_simulated() throws Exception {
    OssIndexAuditTask taskSpy = buildAuditTaskSpy(true);
    taskSpy.audit();
    verify(ossIndexClientMock, never()).requestComponentReports(anyList());
  }

  private OssIndexAuditTask buildAuditTaskSpy(boolean isSimulated) {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("java");
    project.getRepositories().mavenCentral();

    DependencyHandler dependencyHandler = project.getDependencies();
    dependencyHandler.add(COMPILE_CLASSPATH_CONFIGURATION_NAME, COMMONS_COLLECTIONS_DEPENDENCY);

    OssIndexPluginExtension extension = new OssIndexPluginExtension(project);
    extension.setSimulationEnabled(isSimulated);
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

      report.setVulnerabilities(Arrays.asList(vulnerability));
    }

    Map<PackageUrl, ComponentReport> response = ImmutableMap.of(COMMONS_COLLECTIONS_PURL, report);
    when(ossIndexClientMock.requestComponentReports(eq(Arrays.asList(COMMONS_COLLECTIONS_PURL)))).thenReturn(response);
  }
}
