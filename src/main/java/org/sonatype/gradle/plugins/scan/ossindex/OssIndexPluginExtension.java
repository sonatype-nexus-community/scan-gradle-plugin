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

import org.gradle.api.Project;

public class OssIndexPluginExtension
{
  private String username = "";

  private String password = "";

  private boolean useCache = true;

  private String cacheDirectory = "";

  /**
   * It must follow the duration format from <a href=
   * "https://www.javadoc.io/doc/joda-time/joda-time/2.10.4/org/joda/time/Duration.html#parse-java.lang.String-">
   * Duration</a>
   */
  private String cacheExpiration = "";

  public OssIndexPluginExtension(Project project) {
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
}
