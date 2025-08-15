package org.sonatype.gradle.plugins.scan.common;

import javax.inject.Inject;

import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;

public class VariantAttributeDisambiguationRule
    implements AttributeDisambiguationRule<String>
{
  private final String variantValue;

  @Inject
  public VariantAttributeDisambiguationRule(String variantValue) {
    this.variantValue = variantValue;
  }

  @Override
  public void execute(MultipleCandidatesDetails<String> details) {
    if (details.getCandidateValues().contains(variantValue)) {
      details.closestMatch(variantValue);
    }
  }
}
