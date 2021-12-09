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
package org.sonatype.gradle.plugins.scan.nexus.iq.api;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestInterceptor;
import feign.RequestLine;
import feign.RequestTemplate;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

public interface NexusIqApi
{
  @RequestLine("GET /api/v2/applications/organization/{organizationId}")
  ApplicationList getApplicationsByOrganizationId(@Param("organizationId") String organizationId);

  @RequestLine("POST /api/v2/applications")
  @Headers("Content-Type: application/json")
  void createApplication(Application application);

  public static NexusIqApi build(String serverUrl, String username, String password, String userAgent) {
    return Feign.builder()
        .decoder(new GsonDecoder())
        .encoder(new GsonEncoder())
        .requestInterceptor(new BasicAuthRequestInterceptor(username, password))
        .requestInterceptor(new RequestInterceptor()
        {
          @Override
          public void apply(RequestTemplate template) {
            template.header("User-Agent", userAgent);
          }
        })
        .target(NexusIqApi.class, serverUrl);
  }
}
