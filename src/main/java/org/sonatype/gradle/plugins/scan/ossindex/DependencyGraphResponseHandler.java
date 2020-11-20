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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedDependency;

public class DependencyGraphResponseHandler
    implements OssIndexResponseHandler
{
  private static final String DEPENDENCY_PREFIX = "+--- ";

  private static final String REPEATED_MARKER = "(*)";

  private  final OssIndexPluginExtension extension;

  public DependencyGraphResponseHandler(OssIndexPluginExtension extension) {
    this.extension = extension;
  }

  @Override
  public boolean handleOssIndexResponse(
      Set<ResolvedDependency> dependencies,
      Map<ResolvedDependency, PackageUrl> dependenciesMap,
      Map<PackageUrl, ComponentReport> response) {

    log.info(BannerUtils.createBanner());
    log.info("Checking vulnerabilities in {} dependencies", dependenciesMap.size());

    boolean hasVulnerabilities = false;

    Set<PackageUrl> processedPackageUrls = new HashSet<>();
    for (ResolvedDependency dependency : dependencies) {
      boolean vulnerable =
          logWithVulnerabilities(dependency, dependenciesMap, response, processedPackageUrls, DEPENDENCY_PREFIX);
      if (vulnerable) {
        hasVulnerabilities = true;
      }
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
    vulnerabilities.sort(Comparator.comparing(ComponentReportVulnerability::getCvssScore).reversed());

    StringBuilder vulnerabilitiesText = new StringBuilder()
        .append(vulnerabilities.size())
        .append(" vulnerabilities detected");

    vulnerabilitiesText.append(vulnerabilities.stream()
        .map(vulnerability -> handleComponentReportVulnerability(vulnerability, prefix))
        .collect(Collectors.joining("")));

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

  private String getDependencyId(ResolvedDependency dependency) {
    ModuleVersionIdentifier moduleVersionId = dependency.getModule().getId();
    return moduleVersionId.getGroup() + ":" + moduleVersionId.getName() + ":" + moduleVersionId.getVersion();
  }

  private String handleComponentReportVulnerability(ComponentReportVulnerability vulnerability, String prefix) {
    String indent = System.lineSeparator() +
        StringUtils.replaceOnce(prefix, DEPENDENCY_PREFIX, StringUtils.repeat(" ", DEPENDENCY_PREFIX.length()));

    StringBuilder builder = new StringBuilder()
        .append(vulnerability.getTitle());

    Float cvssScore = vulnerability.getCvssScore();
    if (cvssScore != null) {
      builder.append(" (")
          .append(cvssScore)
          .append("/10")
          .append(", ")
          .append(VulnerabilityUtils.getAssessment(cvssScore))
          .append(")");
    }

    builder.append(": ").append(vulnerability.getReference());

    String vulnerabilityText = builder.toString();
    if (extension.isColorEnabled()) {
      vulnerabilityText = VulnerabilityUtils.addColorBasedOnCvssScore(cvssScore, vulnerabilityText);
    }
    return indent + vulnerabilityText;
  }
}
