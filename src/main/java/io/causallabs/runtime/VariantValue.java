package io.causallabs.runtime;

public class VariantValue {

  public VariantValue(
      String featureName, String featureField, Object featureValue, String interfaceType) {
    this.featureName = featureName;
    this.featureField = featureField;
    this.featureValue = featureValue;
    this.interfaceType = interfaceType;
  }

  public final String featureName;
  public final String featureField;
  public Object featureValue;
  public final String interfaceType;
}
