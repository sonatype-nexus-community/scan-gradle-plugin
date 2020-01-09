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
package org.sonatype.gradle.plugins.scan.nexus.iq;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.PolicyAction;

import org.gradle.api.Project;

public class NexusIqPluginExtension
{
  private String stage;

  private String scanFolderPath;

  private String resultFilePath;

  private String username;

  private String password;

  private String serverUrl;

  private String applicationId;

  private boolean simulationEnabled;

  private String simulatedPolicyActionId;

  public NexusIqPluginExtension(Project project) {
    stage = Stage.ID_BUILD;
    simulationEnabled = false;
    simulatedPolicyActionId = PolicyAction.NONE.toString();
    scanFolderPath = project.getBuildDir() + "/sonatype-clm/";
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }


  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getScanFolderPath() {
    return scanFolderPath;
  }

  public void setScanFolderPath(String scanFolderPath) {
    this.scanFolderPath = scanFolderPath;
  }

  public String getResultFilePath() {
    return resultFilePath;
  }

  public void setResultFilePath(final String resultFilePath) {
    this.resultFilePath = resultFilePath;
  }

  public String getStage() {
    return stage;
  }

  public void setStage(final String stage) {
    this.stage = stage;
  }

  public boolean isSimulationEnabled() {
    return simulationEnabled;
  }

  public void setSimulationEnabled(final boolean simulationEnabled) {
    this.simulationEnabled = simulationEnabled;
  }

  public String getSimulatedPolicyActionId() {
    return simulatedPolicyActionId;
  }

  public void setSimulatedPolicyActionId(final String simulatedPolicyActionId) {
    this.simulatedPolicyActionId = simulatedPolicyActionId;
  }
}
