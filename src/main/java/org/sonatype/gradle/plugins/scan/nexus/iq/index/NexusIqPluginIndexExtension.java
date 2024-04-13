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
package org.sonatype.gradle.plugins.scan.nexus.iq.index;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;

public class NexusIqPluginIndexExtension
{
  private boolean allConfigurations;

  private Set<String> modulesExcluded;

  private Map<String, String> variantAttributes;

  private boolean excludeCompileOnly;

  public NexusIqPluginIndexExtension(Project project) {
    modulesExcluded = Collections.emptySet();
    variantAttributes = Collections.emptyMap();
  }

  public boolean isAllConfigurations() {
    return allConfigurations;
  }

  public void setAllConfigurations(boolean allConfigurations) {
    this.allConfigurations = allConfigurations;
  }

  public Set<String> getModulesExcluded() {
    return modulesExcluded;
  }

  public void setModulesExcluded(Set<String> modulesExcluded) {
    this.modulesExcluded = modulesExcluded;
  }

  public Map<String, String> getVariantAttributes() {
    return variantAttributes;
  }

  public void setVariantAttributes(Map<String, String> variantAttributes) {
    this.variantAttributes = variantAttributes;
  }

  public boolean isExcludeCompileOnly() {
    return excludeCompileOnly;
  }

  public void setExcludeCompileOnly(boolean excludeCompileOnly) {
    this.excludeCompileOnly = excludeCompileOnly;
  }
}
