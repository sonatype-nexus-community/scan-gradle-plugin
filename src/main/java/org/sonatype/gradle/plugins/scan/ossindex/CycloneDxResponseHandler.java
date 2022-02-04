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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;

import nexus.shadow.org.cyclonedx.BomGeneratorFactory;
import nexus.shadow.org.cyclonedx.CycloneDxSchema;
import nexus.shadow.org.cyclonedx.generators.json.BomJsonGenerator;
import nexus.shadow.org.cyclonedx.model.Bom;
import nexus.shadow.org.cyclonedx.model.Component;
import nexus.shadow.org.cyclonedx.model.ExtensibleType;
import nexus.shadow.org.cyclonedx.model.Extension;
import nexus.shadow.org.cyclonedx.model.Extension.ExtensionType;
import nexus.shadow.org.cyclonedx.model.Source;
import nexus.shadow.org.cyclonedx.model.vulnerability.Rating;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability10;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability10.Score;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability10.Severity;
import org.gradle.api.artifacts.ResolvedDependency;

public class CycloneDxResponseHandler
    implements OssIndexResponseHandler
{
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
    boolean hasVulnerabilities = false;
    int dependenciesCount = dependenciesMap.size();

    if (!extension.isShowAll()) {
      dependenciesCount = (int) dependenciesMap.values().parallelStream()
          .filter(packageUrl -> !response.get(packageUrl).getVulnerabilities().isEmpty())
          .count();

      if (dependenciesCount == 0) {
        log.info("No vulnerabilities found!");
      }
      else {
        log.info("Found vulnerabilities in {} dependencies", dependenciesCount);
      }
    }

    Bom bom = new Bom();

    for (Entry<ResolvedDependency, PackageUrl> entry : dependenciesMap.entrySet()) {
      PackageUrl packageUrl = entry.getValue();
      ComponentReport componentReport = response.get(packageUrl);

      if (componentReport != null) {
        List<ComponentReportVulnerability> vulnerabilities = componentReport.getVulnerabilities();
        if (!vulnerabilities.isEmpty() || extension.isShowAll()) {
          Component component = new Component();
          component.setType(Component.Type.LIBRARY);
          component.setGroup(packageUrl.getNamespaceAsString());
          component.setName(packageUrl.getName());
          component.setVersion(packageUrl.getVersion());
          component.setPurl(packageUrl.toString());
          component.setBomRef(packageUrl.toString());

          List<ExtensibleType> extensions = new ArrayList<>();

          for (ComponentReportVulnerability vulnerability : vulnerabilities) {
            Vulnerability10 vulnerability10 = new Vulnerability10(component.getGroup(), component.getName());
            vulnerability10.setId(vulnerability.getId());
            vulnerability10.setRef(packageUrl.toString());
            vulnerability10.setDescription(vulnerability.getDescription());

            Score score = new Score();
            Double base = Double.valueOf(vulnerability.getCvssScore());
            score.setBase(base);

            Rating rating = new Rating();
            rating.setScore(score);
            rating.setSeverity(Severity.fromString(VulnerabilityUtils.getAssessment(vulnerability.getCvssScore())));
            rating.setVector(Objects.toString(vulnerability.getCvssVector(), "Unspecified"));
            vulnerability10.setRatings(Collections.singletonList(rating));

            Source source = new Source();
            source.setName("OSS Index");
            try {
              source.setUrl(vulnerability.getReference().toURL());
            }
            catch (MalformedURLException e) {
              log.error("Error processing the vulnerability URL: {}", vulnerability.getReference());
            }
            vulnerability10.setSource(source);

            extensions.add(vulnerability10);
          }

          Extension extension = new Extension(ExtensionType.VULNERABILITIES, extensions);
          bom.addComponent(component);
          bom.add(ExtensionType.VULNERABILITIES.getTypeName(), extension);
        }

        if (!vulnerabilities.isEmpty()) {
          hasVulnerabilities = true;
        }
      }
    }

    BomJsonGenerator generator = BomGeneratorFactory.createJson(CycloneDxSchema.Version.VERSION_13, bom);
    log.info(generator.toJsonString());

    return hasVulnerabilities;
  }
}
