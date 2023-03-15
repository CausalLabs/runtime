package io.causallabs.runtime;

public class VariantValue {

  public VariantValue(String featureName, String featureField, Object featureValue) {
    this.featureName = featureName;
    this.featureField = featureField;
    this.featureValue = featureValue;
  }

  public final String featureName;
  public final String featureField;
  public Object featureValue;
}
