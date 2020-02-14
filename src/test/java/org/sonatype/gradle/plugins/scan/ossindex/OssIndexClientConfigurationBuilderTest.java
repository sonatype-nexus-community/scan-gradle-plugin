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

import java.nio.file.Paths;

import org.sonatype.ossindex.service.client.OssindexClientConfiguration;
import org.sonatype.ossindex.service.client.cache.DirectoryCache;
import org.sonatype.ossindex.service.client.cache.DirectoryCache.Configuration;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OssIndexClientConfigurationBuilderTest
{
  private OssIndexClientConfigurationBuilder builder;

  @Before
  public void setup() {
    builder = new OssIndexClientConfigurationBuilder();
  }

  @Test
  public void testBuild_nullExtension() {
    OssindexClientConfiguration expected = new OssindexClientConfiguration();
    OssindexClientConfiguration result = builder.build(null);

    assertThat(result).isNotNull();
    assertThat(result).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void testBuild_defaultValuesExtension() {
    OssindexClientConfiguration expected = new OssindexClientConfiguration();
    OssindexClientConfiguration result = builder.build(new OssIndexPluginExtension(null));

    assertThat(result).isNotNull();
    assertThat(result).isEqualToIgnoringGivenFields(expected, "cacheConfiguration");
    assertThat(result.getCacheConfiguration()).isInstanceOf(DirectoryCache.Configuration.class);
  }

  @Test
  public void testBuild_customValuesExtension() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setUsername("username");
    extension.setPassword("password");
    extension.setUseCache(false);

    OssindexClientConfiguration result = builder.build(extension);

    assertThat(result).isNotNull();
    assertThat(result.getAuthConfiguration()).isNotNull();
    assertThat(result.getAuthConfiguration().getUsername()).isEqualTo(extension.getUsername());
    assertThat(result.getAuthConfiguration().getPassword()).isEqualTo(extension.getPassword());
    assertThat(result.getCacheConfiguration()).isNull();
  }

  @Test
  public void testBuild_customCacheValuesExtension() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setCacheDirectory("/fake-dir/");
    extension.setCacheExpiration("PT60S");

    OssindexClientConfiguration result = builder.build(extension);

    assertThat(result).isNotNull();
    assertThat(result.getCacheConfiguration()).isInstanceOf(Configuration.class);

    Configuration cacheConfiguration = (Configuration) result.getCacheConfiguration();
    assertThat(cacheConfiguration.getBaseDir()).isEqualTo(Paths.get(extension.getCacheDirectory()));
    assertThat(cacheConfiguration.getExpireAfter()).isEqualTo(Duration.parse(extension.getCacheExpiration()));
  }

  @Test
  public void testBuild_invalidExpirationCacheExtension() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setCacheExpiration("FAKE");

    Duration expectedDuration = new Configuration().getExpireAfter();
    OssindexClientConfiguration result = builder.build(extension);

    assertThat(result).isNotNull();
    assertThat(result.getCacheConfiguration()).isInstanceOf(Configuration.class);

    Configuration cacheConfiguration = (Configuration) result.getCacheConfiguration();
    assertThat(cacheConfiguration.getExpireAfter()).isEqualTo(expectedDuration);
  }
}
