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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.artifacts.ResolvedDependency;

public class DefaultResponseHandler
    implements OssIndexResponseHandler
{
  private final OssIndexPluginExtension extension;

  public DefaultResponseHandler(OssIndexPluginExtension extension) {
    this.extension = extension;
  }

  @Override
  public boolean handleOssIndexResponse(
      final Set<ResolvedDependency> dependencies,
      final Map<ResolvedDependency, PackageUrl> dependenciesMap,
      final Map<PackageUrl, ComponentReport> response)
  {
    log.info(BannerUtils.createBanner());
    log.info("Checking vulnerabilities in {} dependencies", dependenciesMap.size());

    boolean hasVulnerabilities = false;
    int index = 1;

    for (Entry<ResolvedDependency, PackageUrl> entry : dependenciesMap.entrySet()) {
      PackageUrl packageUrl = entry.getValue();
      ComponentReport componentReport = response.get(packageUrl);

      List<ComponentReportVulnerability> vulnerabilities = getSortedVulnerabilities(componentReport);
      log.info(getProcessingPackageUrlString(packageUrl, vulnerabilities, index++, dependenciesMap.size()));
      for (ComponentReportVulnerability vulnerability : vulnerabilities) {
        log.info((getVulnerabilityDetailsString(vulnerability)));
      }

      boolean vulnerable = !vulnerabilities.isEmpty();
      if (vulnerable) {
        hasVulnerabilities = true;
      }
    }

    return hasVulnerabilities;
  }

  private List<ComponentReportVulnerability> getSortedVulnerabilities(ComponentReport componentReport) {
    List<ComponentReportVulnerability> vulnerabilities = new ArrayList<>();
    if (!Objects.isNull(componentReport)) {
      vulnerabilities = componentReport.getVulnerabilities();
      vulnerabilities.sort(Comparator.comparing(ComponentReportVulnerability::getCvssScore).reversed());

    }
    return vulnerabilities;
  }

  private String getProcessingPackageUrlString(
      PackageUrl packageUrl,
      List<ComponentReportVulnerability> vulnerabilities,
      int index,
      int totalComponents)
  {
    String packageUrlProcessingText = "[" + index + "/" + totalComponents + "] - " + packageUrl + " - ";

    if (vulnerabilities.isEmpty()) {
      packageUrlProcessingText += "No vulnerabilities found!";
    }
    else if (vulnerabilities.size() == 1) {
      packageUrlProcessingText += "1 vulnerability found!";
    }
    else {
      packageUrlProcessingText += vulnerabilities.size() + " vulnerabilities found";
    }


    if (extension.isColorEnabled()) {
      if (vulnerabilities.isEmpty()) {
        packageUrlProcessingText =
            VulnerabilityUtils.addColor(VulnerabilityUtils.ASCII_COLOR_GREEN, packageUrlProcessingText);
      }
      else {
        Float maxCvssScore = vulnerabilities.stream()
            .map(ComponentReportVulnerability::getCvssScore)
            .max(Comparator.naturalOrder())
            .orElse(0F);

        packageUrlProcessingText =
            VulnerabilityUtils.addColorBasedOnCvssScore(maxCvssScore, packageUrlProcessingText);
      }
    }

    return packageUrlProcessingText;
  }

  private String getVulnerabilityDetailsString(ComponentReportVulnerability vulnerability) {
    Float cvssScore = vulnerability.getCvssScore();

    return new StringBuilder()
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   Vulnerability Title:  ")).append(vulnerability.getTitle())
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   ID:  ")).append(vulnerability.getId())
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   Description:  "))
        .append(StringUtils.abbreviate(vulnerability.getDescription().replaceAll("\n", " "), 140))
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   CVSS Score:  ")).append("(").append(vulnerability.getCvssScore()).append("/10")
        .append(", ").append(VulnerabilityUtils.getAssessment(cvssScore)).append(")")
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   CVSS Vector:  "))
        .append(Objects.toString(vulnerability.getCvssVector(), "Unspecified"))
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   CVE:  ")).append(Objects.toString(vulnerability.getCve(), "Unspecified"))
        .append(System.lineSeparator())

        .append(addColour(cvssScore, "   Reference:  ")).append(vulnerability.getReference())
        .append(System.lineSeparator()).toString();
  }

  private String addColour(Float cvssScore, String text) {
    if (extension.isColorEnabled()) {
      return VulnerabilityUtils.addColorBasedOnCvssScore(cvssScore, text);
    }
    else {
      return text;
    }
  }
}