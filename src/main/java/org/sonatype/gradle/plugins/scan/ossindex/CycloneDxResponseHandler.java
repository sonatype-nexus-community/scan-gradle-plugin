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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.gradle.plugins.scan.common.PluginVersionUtils;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;

import nexus.shadow.org.cyclonedx.BomGeneratorFactory;
import nexus.shadow.org.cyclonedx.CycloneDxSchema;
import nexus.shadow.org.cyclonedx.generators.json.BomJsonGenerator;
import nexus.shadow.org.cyclonedx.model.Bom;
import nexus.shadow.org.cyclonedx.model.Component;
import nexus.shadow.org.cyclonedx.model.Tool;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability.Reference;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability.Source;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability.Version;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability.Version.Status;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.artifacts.ResolvedDependency;

public class CycloneDxResponseHandler
    implements OssIndexResponseHandler
{
  public static final String FILE_NAME_OUTPUT = "oss-index-cyclonedx.json";

  private final OssIndexPluginExtension extension;

  public CycloneDxResponseHandler(OssIndexPluginExtension extension) {
    this.extension = extension;
  }

  @Override
  public boolean handleOssIndexResponse(
      Set<ResolvedDependency> dependencies,
      Map<ResolvedDependency, PackageUrl> dependenciesMap,
      Map<PackageUrl, ComponentReport> response)
  {
    int dependenciesCount = dependenciesMap.size();

    if (!extension.isShowAll()) {
      dependenciesCount = (int) dependenciesMap.values().parallelStream()
          .filter(packageUrl -> !response.get(packageUrl).getVulnerabilities().isEmpty())
          .count();

      if (dependenciesCount == 0) {
        log.info("No vulnerabilities found!");
        return false;
      }
      else {
        log.info("Found vulnerabilities in {} dependencies", dependenciesCount);
      }
    }

    Bom bom = new Bom();
    List<Vulnerability> vulnerabilities = new ArrayList<>();

    for (Entry<ResolvedDependency, PackageUrl> entry : dependenciesMap.entrySet()) {
      PackageUrl packageUrl = entry.getValue();
      ComponentReport componentReport = response.get(packageUrl);

      if (componentReport != null) {
        List<ComponentReportVulnerability> componentVulnerabilities = componentReport.getVulnerabilities();

        if (!componentVulnerabilities.isEmpty() || extension.isShowAll()) {
          Component component = buildComponent(packageUrl);

          for (ComponentReportVulnerability componentVulnerability : componentVulnerabilities) {
            Vulnerability vulnerability = new Vulnerability();
            vulnerability.setBomRef(component.getBomRef());
            vulnerability.setId(componentVulnerability.getId());

            addSource(componentVulnerability, vulnerability);

            addReferences(componentVulnerability, vulnerability);

            addRating(componentVulnerability, vulnerability);

            if (NumberUtils.isDigits(componentVulnerability.getCwe())) {
              vulnerability.addCwe(Integer.parseInt(componentVulnerability.getCwe()));
            }

            vulnerability.setDescription(componentVulnerability.getDescription());

            addToolDetails(vulnerability);

            addAffectedVersionRanges(component, componentVulnerability, vulnerability);

            vulnerabilities.add(vulnerability);
          }

          bom.addComponent(component);
        }
      }
    }

    if (!vulnerabilities.isEmpty()) {
      bom.setVulnerabilities(vulnerabilities);
    }

    generateFile(bom);

    return !vulnerabilities.isEmpty();
  }

  private Component buildComponent(PackageUrl packageUrl) {
    Component component = new Component();
    component.setType(Component.Type.LIBRARY);
    component.setGroup(packageUrl.getNamespaceAsString());
    component.setName(packageUrl.getName());
    component.setVersion(packageUrl.getVersion());
    component.setPurl(packageUrl.toString());
    component.setBomRef(packageUrl.toString());
    return component;
  }

  private void addSource(ComponentReportVulnerability componentVulnerability, Vulnerability vulnerability) {
    Source source = new Source();
    source.setName("OSS Index");
    source.setUrl(componentVulnerability.getReference().toString());
    vulnerability.setSource(source);
  }

  private void addReferences(ComponentReportVulnerability componentVulnerability, Vulnerability vulnerability) {
    List<Reference> references = new ArrayList<>();

    if (StringUtils.isNotBlank(componentVulnerability.getCve())) {
      Reference reference = new Reference();
      reference.setId(componentVulnerability.getCve());
      references.add(reference);
    }

    componentVulnerability.getExternalReferences().forEach(externalReference -> {
      Reference reference = new Reference();
      Source externalSource = new Source();
      externalSource.setUrl(externalReference.toString());
      reference.setSource(externalSource);
      references.add(reference);
    });

    vulnerability.setReferences(references);
  }

  private void addRating(ComponentReportVulnerability componentVulnerability, Vulnerability vulnerability) {
    Rating rating = new Rating();
    rating.setScore(Double.valueOf(componentVulnerability.getCvssScore()));
    rating.setSeverity(Severity
        .fromString(VulnerabilityUtils.getAssessment(componentVulnerability.getCvssScore()).toLowerCase(Locale.ROOT)));
    rating.setVector(Objects.toString(componentVulnerability.getCvssVector(), "Unspecified"));
    vulnerability.addRating(rating);
  }

  private void addToolDetails(Vulnerability vulnerability) {
    Tool tool = new Tool();
    tool.setVendor("Sonatype");
    tool.setName("Scan Gradle Plugin (aka Sherlock Trunks)");
    tool.setVersion(PluginVersionUtils.getPluginVersion());
    vulnerability.setTools(Collections.singletonList(tool));
  }

  private void addAffectedVersionRanges(
      Component component,
      ComponentReportVulnerability componentVulnerability,
      Vulnerability vulnerability)
  {
    if (componentVulnerability.getVersionRanges() != null
        && !componentVulnerability.getVersionRanges().isEmpty()) {
      List<Version> versions = componentVulnerability.getVersionRanges().stream().map(range -> {
        Version version = new Version();
        version.setRange(range);
        version.setStatus(Status.AFFECTED);
        return version;
      }).collect(Collectors.toList());

      Affect affect = new Affect();
      affect.setRef(component.getBomRef());
      affect.setVersions(versions);
      vulnerability.setAffects(Collections.singletonList(affect));
    }
  }

  private void generateFile(Bom bom) {
    BomJsonGenerator generator = BomGeneratorFactory.createJson(CycloneDxSchema.Version.VERSION_14, bom);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(FILE_NAME_OUTPUT)))) {
      writer.write(generator.toJsonString());
      writer.flush();
      log.info("CycloneDX SBOM file: {}", FILE_NAME_OUTPUT);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Error generating the CycloneDX SBOM file", e);
    }
  }
}
