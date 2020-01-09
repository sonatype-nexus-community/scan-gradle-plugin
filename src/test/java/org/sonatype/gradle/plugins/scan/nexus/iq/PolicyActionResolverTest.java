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
package org.sonatype.gradle.plugins.scan.nexus.iq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.nexus.api.iq.Action;
import com.sonatype.nexus.api.iq.PolicyAlert;
import com.sonatype.nexus.api.iq.PolicyFact;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyActionResolverTest
{
  private PolicyActionResolver resolver;

  @Before
  public void setup() {
    resolver = new PolicyActionResolver();
  }

  @Test
  public void testResolve_None() {
    testResolve(null, PolicyAction.NONE);
  }

  @Test
  public void testResolve_Fail() {
    testResolve(Action.ID_WARN, PolicyAction.WARN);
  }

  @Test
  public void testResolve_Warn() {
    testResolve(Action.ID_FAIL, PolicyAction.FAIL);
  }

  private void testResolve(String actionId, PolicyAction outcome) {
    List<PolicyAlert> alerts = new ArrayList<>();
    if (actionId != null) {
      alerts.add(new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10, Collections.emptyList()),
          asList(new Action(actionId))));
    }
    assertThat(resolver.resolve(alerts)).isEqualTo(outcome);
  }
}
