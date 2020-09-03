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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.scan.module.model.Module;
import com.sonatype.nexus.api.common.Authentication;
import com.sonatype.nexus.api.common.ServerConfig;
import com.sonatype.nexus.api.iq.Action;
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation;
import com.sonatype.nexus.api.iq.PolicyAlert;
import com.sonatype.nexus.api.iq.PolicyFact;
import com.sonatype.nexus.api.iq.ProprietaryConfig;
import com.sonatype.nexus.api.iq.internal.InternalIqClient;
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder;
import com.sonatype.nexus.api.iq.scan.ScanResult;

import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusIqScanTask
    extends DefaultTask
{
  private final Logger log = LoggerFactory.getLogger(NexusIqScanTask.class);

  private static final String MINIMAL_SERVER_VERSION_REQUIRED = "1.69.0";

  private final NexusIqPluginExtension extension;

  private DependenciesFinder dependenciesFinder;

  public NexusIqScanTask() {
    extension = getProject().getExtensions().getByType(NexusIqPluginExtension.class);
    dependenciesFinder = new DependenciesFinder();
  }

  @TaskAction
  public void scan() {
    try {
      final ApplicationPolicyEvaluation applicationPolicyEvaluation;

      if (extension.isSimulationEnabled()) {
        log.info("Simulating scan...");

        List<PolicyAlert> alerts = new ArrayList<>();
        if (extension.getSimulatedPolicyActionId() != null) {
          alerts.add(new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10, Collections.emptyList()),
              Collections.singletonList(new Action(extension.getSimulatedPolicyActionId()))));
        }

        applicationPolicyEvaluation =
            new ApplicationPolicyEvaluation(0, 0, 0, 0, 0, 0, 0, 0, alerts, "simulated/report");
      }
      else {
        InternalIqClient iqClient = InternalIqClientBuilder.create()
            .withServerConfig(new ServerConfig(new URI(getServerUrl()),
                    new Authentication(extension.getUsername(), extension.getPassword())))
            .withLogger(log)
            .build();

        iqClient.validateServerVersion(MINIMAL_SERVER_VERSION_REQUIRED);

        if (!iqClient.verifyOrCreateApplication(extension.getApplicationId())) {
          throw new IllegalArgumentException(String.format(
              "Application ID %s doesn't exist and couldn't be created or the user %s doesn't have the "
                  + "'Application Evaluator' role for that application.",
              extension.getApplicationId(), extension.getUsername()));
        }

        ProprietaryConfig proprietaryConfig =
            iqClient.getProprietaryConfigForApplicationEvaluation(extension.getApplicationId());

        File scanFolder = new File(extension.getScanFolderPath());
        List<Module> modules = dependenciesFinder.findModules(getProject(), extension.isAllConfigurations());

        ScanResult scanResult = iqClient.scan(extension.getApplicationId(), proprietaryConfig, new Properties(),
            Collections.emptyList(), scanFolder, Collections.emptyMap(), modules);

        applicationPolicyEvaluation =
            iqClient.evaluateApplication(extension.getApplicationId(), extension.getStage(), scanResult, scanFolder);
      }

      PolicyActionResolver resolver = new PolicyActionResolver();
      PolicyAction policyAction = resolver.resolve(applicationPolicyEvaluation.getPolicyAlerts());

      logReport(policyAction, applicationPolicyEvaluation);
    }
    catch (Exception e) {
      throw new GradleException("Could not scan the project: " + e.getMessage(), e);
    }
  }

  private void logReport(PolicyAction policyAction, ApplicationPolicyEvaluation applicationPolicyEvaluation) {
    StringBuilder message = new StringBuilder();
    for (PolicyAlert alert : applicationPolicyEvaluation.getPolicyAlerts()) {
      PolicyFact trigger = alert.getTrigger();
      for (Action action : alert.getActions()) {
        String actionTypeId = action.getActionTypeId();
        if (Action.ID_FAIL.equals(actionTypeId)) {
          message.append("Sonatype IQ Server reports policy failing due to ").append(trigger).append("\n");
        }
        else if (Action.ID_WARN.equals(actionTypeId)) {
          message.append("Sonatype IQ Server reports policy warning due to ").append(trigger).append("\n");
        }
      }
    }

    String reportUrl = applicationPolicyEvaluation.getApplicationCompositionReportUrl();
    message.append(String.format("Policy Action: %s\n", policyAction));
    message.append(String.format("Number of components affected: %s critical, %s severe, %s moderate\n",
        applicationPolicyEvaluation.getCriticalComponentCount(), applicationPolicyEvaluation.getSevereComponentCount(),
        applicationPolicyEvaluation.getModerateComponentCount()));
    message.append(
        String.format("Number of grandfathered policy violations: %s\n",
            applicationPolicyEvaluation.getGrandfatheredPolicyViolationCount()));
    message.append("The detailed report can be viewed online at ").append(reportUrl).append("\n");

    if (PolicyAction.FAIL == policyAction) {
      throw new GradleException(message.toString());
    }
    else if (PolicyAction.WARN == policyAction) {
      log.warn(message.toString());
    }
    else {
      log.info(message.toString());
    }
  }

  @Input
  public String getScanFolderPath() {
    return extension.getScanFolderPath();
  }

  @Input
  public String getUsername() {
    return extension.getUsername();
  }

  @Input
  public String getPassword() {
    return extension.getPassword();
  }

  @Input
  public String getApplicationId() {
    return extension.getApplicationId();
  }

  @Input
  public String getServerUrl() {
    return extension.getServerUrl();
  }

  @Input
  public String getStage() {
    return extension.getStage();
  }

  @Input
  public boolean isAllConfigurations() {
    return extension.isAllConfigurations();
  }

  @VisibleForTesting
  void setDependenciesFinder(DependenciesFinder dependenciesFinder) {
    this.dependenciesFinder = dependenciesFinder;
  }
}
