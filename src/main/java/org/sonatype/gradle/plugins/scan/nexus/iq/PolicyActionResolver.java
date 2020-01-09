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

import java.util.List;

import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.nexus.api.iq.Action;
import com.sonatype.nexus.api.iq.PolicyAlert;

public class PolicyActionResolver
{
  public PolicyAction resolve(List<PolicyAlert> alerts) {
    PolicyAction outcome = PolicyAction.NONE;
    for (PolicyAlert alert : alerts) {
      for (Action action : alert.getActions()) {
        String actionTypeId = action.getActionTypeId();
        if (Action.ID_FAIL.equals(actionTypeId)) {
          outcome = outcome.combine(PolicyAction.FAIL);
        }
        else if (Action.ID_WARN.equals(actionTypeId)) {
          outcome = outcome.combine(PolicyAction.WARN);
        }
      }
    }
    return outcome;
  }
}
