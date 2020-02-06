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
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.goodies.packageurl.PackageUrlBuilder;
import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;
import org.sonatype.ossindex.service.client.OssindexClient;
import org.sonatype.ossindex.service.client.OssindexClientConfiguration;
import org.sonatype.ossindex.service.client.internal.OssindexClientImpl;
import org.sonatype.ossindex.service.client.marshal.GsonMarshaller;
import org.sonatype.ossindex.service.client.marshal.Marshaller;
import org.sonatype.ossindex.service.client.transport.Transport;
import org.sonatype.ossindex.service.client.transport.Transport.TransportException;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OssIndexAuditTask
    extends DefaultTask
{
  private final Logger log = LoggerFactory.getLogger(OssIndexAuditTask.class);

  private final OssIndexPluginExtension extension;

  private final DependenciesFinder dependenciesFinder;

  public OssIndexAuditTask() {
    extension = getProject().getExtensions().getByType(OssIndexPluginExtension.class);
    dependenciesFinder = new DependenciesFinder();
  }

  @TaskAction
  public void audit() {
    boolean hasVulnerabilities = false;

    try (OssindexClient ossIndexClient = buildOssIndexClient()) {

      Map<PackageUrl, ResolvedArtifact> artifactsMap = getProject().getAllprojects().stream()
          .flatMap(project -> dependenciesFinder.findResolvedArtifacts(project).stream())
          .collect(Collectors.toMap(this::packageUrl, artifact -> artifact));

      List<PackageUrl> packageUrls = new ArrayList<>(artifactsMap.keySet());
      log.info("Checking vulnerabilities in {} artifacts", packageUrls.size());

      Map<PackageUrl, ComponentReport> response;

      if (extension.isSimulationEnabled()) {
        response = buildSimulatedResponse(packageUrls);
      }
      else {
        response = ossIndexClient.requestComponentReports(packageUrls);
      }

      hasVulnerabilities = handleOssIndexResponse(artifactsMap, response);
    }
    catch (TransportException e) {
      throw new GradleException("Connection to OSS Index failed, check your credentials: " + e.getMessage(), e);
    }
    catch (UnknownHostException e) {
      throw new GradleException("Connection to OSS Index failed, check your internet status: " + e.getMessage(), e);
    }
    catch (Exception e) {
      throw new GradleException("Could not audit the project: " + e.getMessage(), e);
    }

    if (hasVulnerabilities) {
      throw new GradleException("Vulnerabilities detected, check log output to review them");
    }
  }

  @VisibleForTesting
  OssindexClient buildOssIndexClient() {
    OssindexClientConfiguration clientConfiguration = new OssIndexClientConfigurationBuilder().build(extension);
    Transport transport = new TransportBuilder().build(getProject());
    Marshaller marshaller = new GsonMarshaller();

    return new OssindexClientImpl(clientConfiguration, transport, marshaller);
  }

  private Map<PackageUrl, ComponentReport> buildSimulatedResponse(
      List<PackageUrl> packageUrls) throws URISyntaxException
  {
    Map<PackageUrl, ComponentReport> map = new HashMap<>();

    for (PackageUrl packageUrl : packageUrls) {
      ComponentReport report = new ComponentReport();
      report.setCoordinates(packageUrl);

      if (extension.isSimulatedVulnerabityFound()) {
        ComponentReportVulnerability vulnerability = new ComponentReportVulnerability();
        vulnerability.setTitle("Simulated");
        vulnerability.setCvssScore(4f);
        vulnerability.setReference(new URI("http://test/123"));
        report.setVulnerabilities(Arrays.asList(vulnerability));
      }

      map.put(packageUrl, report);
    }

    return map;
  }

  private boolean handleOssIndexResponse(
      Map<PackageUrl, ResolvedArtifact> artifactsMap,
      Map<PackageUrl, ComponentReport> response)
  {
    boolean hasVulnerabilities = false;

    for (Entry<PackageUrl, ComponentReport> entry : response.entrySet()) {
      ResolvedArtifact artifact = artifactsMap.get(entry.getKey());

      List<ComponentReportVulnerability> vulnerabilities = entry.getValue().getVulnerabilities();

      StringBuilder vulnerabilitiesText = new StringBuilder(vulnerabilities.size() + " vulnerabilities detected")
          .append(vulnerabilities.stream()
              .map(this::handleComponentReportVulnerability)
              .collect(Collectors.joining(System.lineSeparator())));

      if (vulnerabilities.isEmpty()) {
        log.info("{}: {}", artifact.getId().getComponentIdentifier().getDisplayName(), vulnerabilitiesText);
      }
      else {
        hasVulnerabilities = true;
        log.error("{}: {}", artifact.getId().getComponentIdentifier().getDisplayName(), vulnerabilitiesText);
      }
    }

    return hasVulnerabilities;
  }

  private PackageUrl packageUrl(ResolvedArtifact artifact) {
    PackageUrlBuilder builder = new PackageUrlBuilder()
        .type("maven")
        .namespace(artifact.getModuleVersion().getId().getGroup())
        .name(artifact.getModuleVersion().getId().getName())
        .version(artifact.getModuleVersion().getId().getVersion());

    if (StringUtils.isNotBlank(artifact.getExtension())) {
      builder.qualifier("extension", artifact.getExtension());
    }

    return builder.build();
  }

  private String handleComponentReportVulnerability(ComponentReportVulnerability vulnerability) {
    StringBuilder builder = new StringBuilder(System.lineSeparator()).append(vulnerability.getTitle());

    if (vulnerability.getCvssScore() != null) {
      builder.append(" (").append(vulnerability.getCvssScore()).append(')');
    }

    builder.append(": ").append(vulnerability.getReference());

    return builder.toString();
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
  public boolean isUseCache() {
    return extension.isUseCache();
  }

  @Input
  public String getCacheDirectory() {
    return extension.getCacheDirectory();
  }

  @Input
  public String getCacheExpiration() {
    return extension.getCacheExpiration();
  }
}
