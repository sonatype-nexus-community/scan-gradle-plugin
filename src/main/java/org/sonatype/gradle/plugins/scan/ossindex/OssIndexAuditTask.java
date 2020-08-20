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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OssIndexAuditTask
    extends DefaultTask
{
  private final Logger log = LoggerFactory.getLogger(OssIndexAuditTask.class);

  private static final String DEPENDENCY_PREFIX = "+--- ";

  private static final String REPEATED_MARKER = "(*)";

  private static final String ASCII_COLOR_RED = "\033[31m";

  private static final String ASCII_COLOR_RESET = "\033[0m";

  private final OssIndexPluginExtension extension;

  private final DependenciesFinder dependenciesFinder;

  public OssIndexAuditTask() {
    extension = getProject().getExtensions().getByType(OssIndexPluginExtension.class);
    dependenciesFinder = new DependenciesFinder();
  }

  @TaskAction
  public void audit() {
    boolean hasVulnerabilities;

    try (OssindexClient ossIndexClient = buildOssIndexClient()) {
      Set<ResolvedDependency> dependencies =
          dependenciesFinder.findResolvedDependencies(getProject(), extension.isAllConfigurations());
      BiMap<ResolvedDependency, PackageUrl> dependenciesMap = HashBiMap.create();

      dependencies.forEach(dependency -> {
        buildDependenciesMap(dependency, dependenciesMap);
      });

      List<PackageUrl> packageUrls = new ArrayList<>(dependenciesMap.values());
      log.info("Checking vulnerabilities in {} dependencies", packageUrls.size());

      Map<PackageUrl, ComponentReport> response;

      if (extension.isSimulationEnabled()) {
        response = buildSimulatedResponse(packageUrls);
      }
      else {
        response = ossIndexClient.requestComponentReports(packageUrls);
      }

      hasVulnerabilities = handleOssIndexResponse(dependencies, dependenciesMap, response);
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

  private void buildDependenciesMap(
      ResolvedDependency dependency,
      BiMap<ResolvedDependency, PackageUrl> mapAccumulator)
  {
    mapAccumulator.forcePut(dependency, toPackageUrl(dependency));

    dependency.getChildren().forEach(child -> {
      mapAccumulator.forcePut(child, toPackageUrl(child));
      buildDependenciesMap(child, mapAccumulator);
    });
  }

  private PackageUrl toPackageUrl(ResolvedDependency dependency) {
    return new PackageUrlBuilder()
        .type("maven")
        .namespace(dependency.getModule().getId().getGroup())
        .name(dependency.getModule().getId().getName())
        .version(dependency.getModule().getId().getVersion())
        .build();
  }

  private boolean handleOssIndexResponse(
      Set<ResolvedDependency> dependencies,
      Map<ResolvedDependency, PackageUrl> dependenciesMap,
      Map<PackageUrl, ComponentReport> response)
  {
    boolean hasVulnerabilities = false;

    Set<PackageUrl> processedPackageUrls = new HashSet<>();
    for (ResolvedDependency dependency : dependencies) {
      hasVulnerabilities =
          logWithVulnerabilities(dependency, dependenciesMap, response, processedPackageUrls, DEPENDENCY_PREFIX);
    }

    if (!dependencies.isEmpty()) {
      log.info("{}{} - if present, dependencies omitted (listed previously)", System.lineSeparator(), REPEATED_MARKER);
    }

    return hasVulnerabilities;
  }

  private boolean logWithVulnerabilities(
      ResolvedDependency dependency,
      Map<ResolvedDependency, PackageUrl> dependenciesMap,
      Map<PackageUrl, ComponentReport> response,
      Set<PackageUrl> processedPackageUrls,
      String prefix)
  {
    PackageUrl packageUrl = dependenciesMap.get(dependency);
    ComponentReport report = response.get(packageUrl);
    List<ComponentReportVulnerability> vulnerabilities =
        report != null ? report.getVulnerabilities() : Collections.emptyList();

    StringBuilder vulnerabilitiesText = new StringBuilder()
        .append(vulnerabilities.size())
        .append(" vulnerabilities detected");
    changeColor(vulnerabilitiesText, ASCII_COLOR_RED);
    vulnerabilitiesText.append(vulnerabilities.stream()
        .map(vulnerability -> handleComponentReportVulnerability(vulnerability, prefix))
        .collect(Collectors.joining("")));
    changeColor(vulnerabilitiesText, ASCII_COLOR_RESET);

    String id = getDependencyId(dependency);
    boolean isRepeated = !processedPackageUrls.add(packageUrl);
    String repeatedMarker = isRepeated && !dependency.getChildren().isEmpty() ? " " + REPEATED_MARKER : "";
    boolean hasVulnerabilities = !vulnerabilities.isEmpty();

    log.info("{}{}{}: {}", prefix, id, repeatedMarker, vulnerabilitiesText);

    if (isRepeated) {
      return hasVulnerabilities;
    }

    Set<ResolvedDependency> childrenSet = new TreeSet<>(
        Comparator.comparing(ResolvedDependency::getModuleGroup)
        .thenComparing(ResolvedDependency::getModuleName)
        .thenComparing(ResolvedDependency::getModuleVersion));
    childrenSet.addAll(dependency.getChildren());

    if (childrenSet.isEmpty()) {
      return hasVulnerabilities;
    }

    return childrenSet.stream()
        .map(child -> logWithVulnerabilities(child, dependenciesMap, response, processedPackageUrls,
            StringUtils.replaceOnce(prefix, DEPENDENCY_PREFIX, "|    ") + DEPENDENCY_PREFIX))
        .collect(Collectors.toList())
        .contains(true) || hasVulnerabilities;

  }

  private void changeColor(StringBuilder vulnerabilitiesText, String asciiCode) {
    if (extension.isColorEnabled()) {
      vulnerabilitiesText.append(asciiCode);
    }
  }

  private String getDependencyId(ResolvedDependency dependency) {
    ModuleVersionIdentifier moduleVersionId = dependency.getModule().getId();
    return moduleVersionId.getGroup() + ":" + moduleVersionId.getName() + ":" + moduleVersionId.getVersion();
  }

  private String handleComponentReportVulnerability(ComponentReportVulnerability vulnerability, String prefix) {
    StringBuilder builder = new StringBuilder(System.lineSeparator())
        .append(StringUtils.replaceOnce(prefix, DEPENDENCY_PREFIX, StringUtils.repeat(" ", DEPENDENCY_PREFIX.length())))
        .append(vulnerability.getTitle());

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

  @Input
  public boolean isAllConfigurations() {
    return extension.isAllConfigurations();
  }
}
