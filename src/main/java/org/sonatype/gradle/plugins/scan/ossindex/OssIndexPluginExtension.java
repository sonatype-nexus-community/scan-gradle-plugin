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

import org.sonatype.ossindex.service.client.transport.AuthConfiguration;
import org.sonatype.ossindex.service.client.transport.ProxyConfiguration;

import groovy.lang.Closure;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;

public class OssIndexPluginExtension
{
  private String username;

  private String password;

  private boolean useCache;

  private String cacheDirectory;

  /**
   * It must follow the duration format from <a href=
   * "https://www.javadoc.io/doc/joda-time/joda-time/2.10.4/org/joda/time/Duration.html#parse-java.lang.String-">
   * Duration</a>
   */
  private String cacheExpiration;

  private boolean allConfigurations;

  private boolean simulationEnabled;

  private boolean simulatedVulnerabilityFound;

  private boolean colorEnabled;

  private boolean dependencyGraph;

  private ProxyConfiguration proxyConfiguration;

  private boolean showAll;

  public OssIndexPluginExtension(Project project) {
    username = "";
    password = "";
    useCache = true;
    cacheDirectory = "";
    cacheExpiration = "";
    simulationEnabled = false;
    simulatedVulnerabilityFound = false;
    colorEnabled = true;
    dependencyGraph = false;
    showAll = false;
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

  public boolean isUseCache() {
    return useCache;
  }

  public void setUseCache(boolean useCache) {
    this.useCache = useCache;
  }

  public String getCacheDirectory() {
    return cacheDirectory;
  }

  public void setCacheDirectory(String cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  public String getCacheExpiration() {
    return cacheExpiration;
  }

  public void setCacheExpiration(String cacheExpiration) {
    this.cacheExpiration = cacheExpiration;
  }

  public boolean isAllConfigurations() {
    return allConfigurations;
  }

  public void setAllConfigurations(boolean allConfigurations) {
    this.allConfigurations = allConfigurations;
  }

  public boolean isSimulationEnabled() {
    return simulationEnabled;
  }

  public void setSimulationEnabled(boolean simulationEnabled) {
    this.simulationEnabled = simulationEnabled;
  }

  public boolean isSimulatedVulnerabilityFound() {
    return simulatedVulnerabilityFound;
  }

  public void setSimulatedVulnerabilityFound(boolean simulatedVulnerabilityFound) {
    this.simulatedVulnerabilityFound = simulatedVulnerabilityFound;
  }

  public boolean isColorEnabled() {
    return colorEnabled;
  }

  public void setColorEnabled(boolean colorEnabled) {
    this.colorEnabled = colorEnabled;
  }

  public boolean isDependencyGraph() {
    return dependencyGraph;
  }

  public void setDependencyGraph(boolean dependencyGraph) {
    this.dependencyGraph = dependencyGraph;
  }

  public ProxyConfiguration getProxyConfiguration() {
    return proxyConfiguration;
  }

  public void setProxyConfiguration(Closure<ProxyConfiguration> closure) {
    proxyConfiguration = new ProxyConfiguration();
    AuthConfiguration authConfiguration = new AuthConfiguration();
    proxyConfiguration.setAuthConfiguration(authConfiguration);

    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.setDelegate(proxyConfiguration);
    closure.call();

    if (StringUtils.isAllBlank(authConfiguration.getUsername(), authConfiguration.getPassword())) {
      proxyConfiguration.setAuthConfiguration(null);
    }
  }

  public boolean isShowAll() {
    return showAll;
  }

  public void setShowAll(boolean showAll) {
    this.showAll = showAll;
  }
}
