package io.causallabs.runtime;

public class VariantValue {

  public VariantValue(
      String featureName,
      String featureField,
      Object featureValue,
      String interfaceType,
      String targetValue) {
    this.featureName = featureName;
    this.featureField = featureField;
    this.featureValue = featureValue;
    this.interfaceType = interfaceType;
    this.targetValue = targetValue;
  }

  public final String featureName;
  public final String featureField;
  public Object featureValue;
  public final String interfaceType;
  public final String targetValue;
}
