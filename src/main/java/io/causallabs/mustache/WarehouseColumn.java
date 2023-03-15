package io.causallabs.mustache;

/** Represents a field (argument, output, or event) of a feature's warehouse table */
public interface WarehouseColumn {

  /** The name of the table this field is stored in */
  public String getTableName();

  /** The name of the column this field is stored in. The FDL field name, but in snake_case. */
  public String getColumnName();

  /** The ORC schema definition */
  public String getOrcType();

  /** The Redshift data type when this field is exposed as a Redshift column */
  public String getRedshiftType();

  /** The Redshift Spectrum data type when this field is exposed as a Redshift column */
  public String getSpectrumType();

  /** The Snowflake data type. */
  public String getSnowflakeType();
}
