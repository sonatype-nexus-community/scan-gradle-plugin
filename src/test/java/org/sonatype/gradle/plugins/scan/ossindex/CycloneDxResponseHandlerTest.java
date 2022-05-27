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

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.goodies.packageurl.PackageUrlBuilder;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;

import nexus.shadow.org.cyclonedx.exception.ParseException;
import nexus.shadow.org.cyclonedx.model.Bom;
import nexus.shadow.org.cyclonedx.model.Component;
import nexus.shadow.org.cyclonedx.model.vulnerability.Vulnerability;
import nexus.shadow.org.cyclonedx.parsers.JsonParser;
import org.gradle.api.artifacts.ResolvedDependency;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonatype.gradle.plugins.scan.ossindex.CycloneDxResponseHandler.FILE_NAME_OUTPUT;

public class CycloneDxResponseHandlerTest
{
  private OssIndexPluginExtension extension;

  private CycloneDxResponseHandler handler;

  @Before
  public void setup() {
    extension = new OssIndexPluginExtension(null);
    extension.setOutputFormat(OutputFormat.JSON_CYCLONE_DX_14);
    handler = new CycloneDxResponseHandler(extension);
  }

  @After
  public void tearDown() {
    new File(FILE_NAME_OUTPUT).delete();
  }

  @Test
  public void testHandleOssIndexResponse_noComponents() {
    handler.handleOssIndexResponse(Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap());

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isFalse();
  }

  @Test
  public void testHandleOssIndexResponse_nonVulnerableComponentNoShowAll() {
    ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
    PackageUrl packageUrl = new PackageUrlBuilder().type("maven").namespace("g").name("a").version("v").build();
    ComponentReport componentReport = new ComponentReport();

    Map<ResolvedDependency, PackageUrl> dependenciesMap = Collections.singletonMap(resolvedDependency, packageUrl);
    Map<PackageUrl, ComponentReport> response = Collections.singletonMap(packageUrl, componentReport);

    handler.handleOssIndexResponse(Collections.emptySet(), dependenciesMap, response);

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isFalse();
  }

  @Test
  public void testHandleOssIndexResponse_nonVulnerableComponentShowAll() throws ParseException {
    extension.setShowAll(true);

    ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
    PackageUrl packageUrl = new PackageUrlBuilder().type("maven").namespace("g").name("a").version("v").build();
    ComponentReport componentReport = new ComponentReport();

    Map<ResolvedDependency, PackageUrl> dependenciesMap = Collections.singletonMap(resolvedDependency, packageUrl);
    Map<PackageUrl, ComponentReport> response = Collections.singletonMap(packageUrl, componentReport);

    handler.handleOssIndexResponse(Collections.emptySet(), dependenciesMap, response);

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isTrue();

    JsonParser jsonParser = new JsonParser();
    Bom bom = jsonParser.parse(file);
    assertThat(bom).isNotNull();
    assertThat(bom.getVulnerabilities()).isNullOrEmpty();

    assertComponent(packageUrl, bom);
  }

  @Test
  public void testHandleOssIndexResponse_vulnerableComponent() throws ParseException {
    ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
    PackageUrl packageUrl = new PackageUrlBuilder().type("maven").namespace("g").name("a").version("v").build();

    ComponentReport componentReport = new ComponentReport();
    componentReport.setCoordinates(packageUrl);

    ComponentReportVulnerability vulnerability = new ComponentReportVulnerability();
    vulnerability.setId("TEST-123");
    vulnerability.setReference(URI.create("https://test.com/TEST-123"));
    vulnerability.setCvssScore(10.0F);
    componentReport.setVulnerabilities(Collections.singletonList(vulnerability));

    Map<ResolvedDependency, PackageUrl> dependenciesMap = Collections.singletonMap(resolvedDependency, packageUrl);
    Map<PackageUrl, ComponentReport> response = Collections.singletonMap(packageUrl, componentReport);

    handler.handleOssIndexResponse(Collections.emptySet(), dependenciesMap, response);

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isTrue();

    JsonParser jsonParser = new JsonParser();
    Bom bom = jsonParser.parse(file);
    assertThat(bom).isNotNull();

    assertComponent(packageUrl, bom);

    assertThat(bom.getVulnerabilities()).hasSize(1);
    Vulnerability resultVulnerability = bom.getVulnerabilities().get(0);
    assertThat(resultVulnerability.getBomRef()).isEqualTo(packageUrl.toString());
    assertThat(resultVulnerability.getId()).isEqualTo("TEST-123");
    assertThat(resultVulnerability.getSource()).isNotNull();
    assertThat(resultVulnerability.getSource().getName()).isEqualTo("OSS Index");
    assertThat(resultVulnerability.getSource().getUrl()).isEqualTo("https://test.com/TEST-123");
    assertThat(resultVulnerability.getRatings()).hasSize(1);
    assertThat(resultVulnerability.getRatings().get(0).getScore()).isEqualTo(10.0);
  }

  private void assertComponent(PackageUrl packageUrl, Bom bom) {
    Component component = new Component();
    component.setType(Component.Type.LIBRARY);
    component.setGroup(packageUrl.getNamespaceAsString());
    component.setName(packageUrl.getName());
    component.setVersion(packageUrl.getVersion());
    component.setPurl(packageUrl.toString());
    component.setBomRef(packageUrl.toString());

    assertThat(bom.getComponents()).containsExactly(component);
  }
}
