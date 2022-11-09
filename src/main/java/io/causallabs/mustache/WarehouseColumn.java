package io.causallabs.mustache;

/** Represents a field (argument, output, or event) of a feature's warehouse table */
public interface WarehouseColumn {

    /** Is this column defined in FDL, or is it implicitly defined by the system */
    public boolean fromFDL();

    /** The name of the table this field is stored in */
    public String getTableName();

    /** The name of the column this field is stored in. The FDL field name, but in snake_case. */
    public String getColumnName();

    /** The ORC schema definition */
    public String getOrcType();

    /** The Redshift data type when this field is exposed as a Redshift column */
    public String getRedshiftType();

    /** Object that represents the comments and deprecation status of the field */
    public Description getDescription();

}