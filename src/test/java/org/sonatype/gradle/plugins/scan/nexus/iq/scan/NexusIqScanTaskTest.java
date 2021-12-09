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
package org.sonatype.gradle.plugins.scan.nexus.iq.scan;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import com.sonatype.nexus.api.common.ServerConfig;
import com.sonatype.nexus.api.exception.IqClientException;
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation;
import com.sonatype.nexus.api.iq.ProprietaryConfig;
import com.sonatype.nexus.api.iq.internal.InternalIqClient;
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder;
import com.sonatype.nexus.api.iq.scan.ScanResult;

import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.Application;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ApplicationList;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.NexusIqApi;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InternalIqClientBuilder.class, NexusIqApi.class})
public class NexusIqScanTaskTest
{
  private static final String USER_AGENT_REGEX =
      "Sonatype_Nexus_Gradle/[^\\s]+ \\(Java [^;]+; [^;]+ [^;]+; Gradle [^;]+\\)";

  @Mock
  private InternalIqClient iqClientMock;

  @Mock
  private NexusIqApi nexusIqApiMock;

  @Mock
  private DependenciesFinder dependenciesFinderMock;

  @Captor
  private ArgumentCaptor<String> userAgentCaptor;

  @Before
  public void setup() throws IqClientException {
    PowerMockito.mockStatic(InternalIqClientBuilder.class);
    PowerMockito.mockStatic(NexusIqApi.class);

    InternalIqClientBuilder builderMock = mock(InternalIqClientBuilder.class);
    when(builderMock.withServerConfig(any(ServerConfig.class))).thenReturn(builderMock);
    when(builderMock.withLogger(any(Logger.class))).thenReturn(builderMock);
    when(builderMock.withUserAgent(userAgentCaptor.capture())).thenReturn(builderMock);
    when(InternalIqClientBuilder.create()).thenReturn(builderMock);

    when(iqClientMock.evaluateApplication(anyString(), anyString(), nullable(ScanResult.class), any(File.class),
        nullable(File.class))).thenReturn(
        new ApplicationPolicyEvaluation(0, 0, 0, 0, 0, 0, 0, 0, 0, Collections.emptyList(), "simulated/report"));
    when(builderMock.build()).thenReturn(iqClientMock);

    when(NexusIqApi.build(eq("http://test"), eq("user"), eq("password"), userAgentCaptor.capture()))
        .thenReturn(nexusIqApiMock);

    when(dependenciesFinderMock.findModules(any(Project.class), eq(false), anySet()))
        .thenReturn(Collections.emptyList());
  }

  @Test
  public void testScan_simulated() throws Exception {
    NexusIqScanTask task = buildScanTask(true);
    task.scan();

    verify(iqClientMock, never()).validateServerVersion(anyString());
    verify(iqClientMock, never()).getProprietaryConfigForApplicationEvaluation(anyString());
    verifyNoInteractions(iqClientMock);
  }

  @Test
  public void testScan_real() throws Exception {
    NexusIqScanTask task = buildScanTask(false);
    task.setDependenciesFinder(dependenciesFinderMock);
    when(iqClientMock.verifyOrCreateApplication(eq(task.getApplicationId()))).thenReturn(true);

    task.scan();

    verify(dependenciesFinderMock).findModules(any(Project.class), eq(false), anySet());
    assertThat(userAgentCaptor.getValue()).matches(USER_AGENT_REGEX);
    verify(iqClientMock).validateServerVersion(anyString());
    verify(iqClientMock).verifyOrCreateApplication(eq(task.getApplicationId()));
    verify(iqClientMock).getProprietaryConfigForApplicationEvaluation(eq(task.getApplicationId()));
    verify(iqClientMock).evaluateApplication(eq(task.getApplicationId()), eq(task.getStage()),
        nullable(ScanResult.class), any(File.class), isNull());
  }

  @Test
  public void testScan_realWithResultFilePath() throws Exception {
    NexusIqScanTask task = buildScanTask(false, "some/path/file.json");
    task.setDependenciesFinder(dependenciesFinderMock);
    when(iqClientMock.verifyOrCreateApplication(eq(task.getApplicationId()))).thenReturn(true);

    task.scan();

    verify(iqClientMock).evaluateApplication(eq(task.getApplicationId()), eq(task.getStage()),
            nullable(ScanResult.class), any(File.class), eq(new File("some/path/file.json")));
  }

  @Test
  public void testScan_realUnableToCreateApp() throws Exception {
    NexusIqScanTask task = buildScanTask(false);
    task.setDependenciesFinder(dependenciesFinderMock);
    when(iqClientMock.verifyOrCreateApplication(eq(task.getApplicationId()))).thenReturn(false);

    assertThatThrownBy(task::scan)
        .isInstanceOf(GradleException.class)
        .hasMessageContaining("Application ID test doesn't exist and couldn't be created or the user user doesn't have"
            + " the 'Application Evaluator' role for that application.");
  }

  @Test
  public void testScan_realCreateAppWithOrganizationWithoutApplications() throws Exception {
    String organizationId = "organizationId";
    doTestScan_realCreateAppWithOrganization(organizationId, false);
  }

  @Test
  public void testScan_realCreateAppWithOrganizationWithApplications() throws Exception {
    String organizationId = "organizationId";
    doTestScan_realCreateAppWithOrganization(organizationId, false, new Application("app1", "app1", organizationId),
        new Application("app2", "app2", organizationId));
  }

  @Test
  public void testScan_realCreateAppWithOrganizationAlreadyExisting() throws Exception {
    String organizationId = "organizationId";
    doTestScan_realCreateAppWithOrganization(organizationId, true, new Application("test", "test", organizationId));
  }

  private void doTestScan_realCreateAppWithOrganization(
      String organizationId,
      boolean isExistingApplication,
      Application... applications) throws Exception
  {
    NexusIqScanTask task = buildScanTask(false, null, null, null, organizationId);
    task.setDependenciesFinder(dependenciesFinderMock);

    ApplicationList applicationList = new ApplicationList();
    applicationList.setApplications(Arrays.asList(applications));
    when(nexusIqApiMock.getApplicationsByOrganizationId(organizationId)).thenReturn(applicationList);

    task.scan();

    verify(iqClientMock, never()).verifyOrCreateApplication(anyString());
    verify(dependenciesFinderMock).findModules(any(Project.class), eq(false), anySet());
    assertThat(userAgentCaptor.getValue()).matches(USER_AGENT_REGEX);
    verify(nexusIqApiMock).getApplicationsByOrganizationId(organizationId);
    if (isExistingApplication) {
      verify(nexusIqApiMock, never()).createApplication(any(Application.class));
    }
    else {
      verify(nexusIqApiMock).createApplication(any(Application.class));
    }
  }

  @Test
  public void testScan_realWithDirIncludes() throws Exception {
    NexusIqScanTask task = buildScanTask(false, null, "dir-include", null, null);
    task.setDependenciesFinder(dependenciesFinderMock);
    when(iqClientMock.verifyOrCreateApplication(eq(task.getApplicationId()))).thenReturn(true);

    task.scan();

    Properties expected = new Properties();
    expected.setProperty("dirIncludes", task.getDirIncludes());

    verify(iqClientMock).scan(eq(task.getApplicationId()), nullable(ProprietaryConfig.class), eq(expected), anyList(),
        any(File.class), anyMap(), anySet(), anyList());

  }

  @Test
  public void testScan_realWithDirExcludes() throws Exception {
    NexusIqScanTask task = buildScanTask(false, null, null, "dir-exclude", null);
    task.setDependenciesFinder(dependenciesFinderMock);
    when(iqClientMock.verifyOrCreateApplication(eq(task.getApplicationId()))).thenReturn(true);

    task.scan();

    Properties expected = new Properties();
    expected.setProperty("dirExcludes", task.getDirExcludes());

    verify(iqClientMock).scan(eq(task.getApplicationId()), nullable(ProprietaryConfig.class), eq(expected), anyList(),
        any(File.class), anyMap(), anySet(), anyList());

  }

  private NexusIqScanTask buildScanTask(boolean isSimulated) {
    return buildScanTask(isSimulated, null);
  }

  private NexusIqScanTask buildScanTask(boolean isSimulated, String resultFilePath) {
    return buildScanTask(isSimulated, resultFilePath, "", "", "");
  }

  private NexusIqScanTask buildScanTask(
      boolean isSimulated,
      String resultFilePath,
      String dirIncludes,
      String dirExcludes,
      String organizationId)
  {
    Project project = ProjectBuilder.builder().build();

    NexusIqPluginScanExtension extension = new NexusIqPluginScanExtension(project);
    extension.setApplicationId("test");
    extension.setOrganizationId(organizationId);
    extension.setServerUrl("http://test");
    extension.setUsername("user");
    extension.setPassword("password");
    extension.setSimulationEnabled(isSimulated);
    extension.setResultFilePath(resultFilePath);
    extension.setDirIncludes(dirIncludes);
    extension.setDirExcludes(dirExcludes);

    project.getExtensions().add("nexusIQScan", extension);
    return project.getTasks().create("nexusIQScan", NexusIqScanTask.class);
  }
}
