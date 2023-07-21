package io.causallabs.mustache;

public interface Field {
  /** Object that represents the comments and deprecation status of the field */
  Description getDescription();

  /** The name of the column this field is stored in. The FDL field name, but in snake_case. */
  String getColumnName();

  /** The ORC schema definition */
  String getOrcType();

  /** The Redshift data type when this field is exposed as a Redshift column */
  String getRedshiftType();

  /** The Redshift Spectrum data type when this field is exposed as a Redshift column */
  String getSpectrumType();

  /** The Snowflake data type. */
  String getSnowflakeType();
}
