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
package org.sonatype.gradle.plugins.scan.common;

import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;

import static org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE;

public class AndroidAttributeDisambiguationRule
    implements AttributeDisambiguationRule<String>
{
  private static final String AAR_TYPE = "aar";

  @Override
  public void execute(MultipleCandidatesDetails<String> details) {
    if (details.getCandidateValues().contains(JAR_TYPE)) {
      details.closestMatch(JAR_TYPE);
    }
    else if (details.getCandidateValues().contains(AAR_TYPE)) {
      details.closestMatch(AAR_TYPE);
    }
  }
}
