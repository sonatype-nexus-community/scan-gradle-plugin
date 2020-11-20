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

import org.sonatype.ossindex.service.client.OssindexClientConfiguration;
import org.sonatype.ossindex.service.client.cache.DirectoryCache;
import org.sonatype.ossindex.service.client.transport.AuthConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OssIndexClientConfigurationBuilder
{
  private final Logger log = LoggerFactory.getLogger(OssIndexClientConfigurationBuilder.class);

  public OssindexClientConfiguration build(OssIndexPluginExtension extension) {
    OssindexClientConfiguration clientConfiguration = new OssindexClientConfiguration();

    if (extension != null) {
      if (StringUtils.isNoneBlank(extension.getUsername(), extension.getPassword())) {
        clientConfiguration
            .setAuthConfiguration(new AuthConfiguration(extension.getUsername(), extension.getPassword()));
      }
      else {
        log.info("Using anonymous request");
      }

      if (extension.isUseCache()) {
        DirectoryCache.Configuration cacheConfig = new DirectoryCache.Configuration();

        if (StringUtils.isNotBlank(extension.getCacheDirectory())) {
          File cacheDirectory = new File(extension.getCacheDirectory());
          if (cacheDirectory != null) {
            cacheConfig.setBaseDir(cacheDirectory.toPath());
          }
        }

        if (StringUtils.isNotBlank(extension.getCacheExpiration())) {
          try {
            cacheConfig.setExpireAfter(Duration.parse(extension.getCacheExpiration()));
          }
          catch (IllegalArgumentException e) {
            log.warn(
                "Invalid cache duration value: {}. Please read https://www.javadoc.io/doc/joda-time/joda-time/2.10.4/org/joda/time/Duration.html#parse-java.lang.String-",
                extension.getCacheExpiration());
          }
        }

        clientConfiguration.setCacheConfiguration(cacheConfig);
      }

      clientConfiguration.setProxyConfiguration(extension.getProxyConfiguration());
    }

    return clientConfiguration;
  }
}
