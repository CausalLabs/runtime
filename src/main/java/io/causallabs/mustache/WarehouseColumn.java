package io.causallabs.mustache;

/** Represents a field (argument, output, or event) of a feature's warehouse table */
public interface WarehouseColumn extends Field {

  /** The name of the table this field is stored in */
  public String getTableName();
}
