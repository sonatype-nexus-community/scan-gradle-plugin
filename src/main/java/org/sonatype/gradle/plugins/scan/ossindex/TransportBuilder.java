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

import org.sonatype.gradle.plugins.scan.common.PluginVersionUtils;
import org.sonatype.ossindex.service.client.internal.VersionSupplier;
import org.sonatype.ossindex.service.client.transport.HttpClientTransport;
import org.sonatype.ossindex.service.client.transport.UserAgentBuilder;
import org.sonatype.ossindex.service.client.transport.UserAgentBuilder.Product;
import org.sonatype.ossindex.service.client.transport.UserAgentSupplier;

import org.gradle.api.Project;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;

public class TransportBuilder
{
  public HttpClientTransport build(Project project) {
    UserAgentSupplier userAgentSupplier = buildUserAgentSupplier(project);
    return new HttpClientTransport(userAgentSupplier);
  }

  @VisibleForTesting
  UserAgentSupplier buildUserAgentSupplier(Project project) {
    return new UserAgentSupplier(new VersionSupplier().get())
    {
      @Override
      protected void customize(UserAgentBuilder builder) {
        builder.product(new Product("Gradle", project.getGradle().getGradleVersion()));
        builder.product(new Product("Gradle-Plugin", PluginVersionUtils.getPluginVersion()));
      }
    };
  }
}
