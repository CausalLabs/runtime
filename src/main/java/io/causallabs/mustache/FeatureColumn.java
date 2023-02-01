package io.causallabs.mustache;

public interface FeatureColumn extends WarehouseColumn {
    /** Is this column defined in FDL, or is it implicitly defined by the system */
    public boolean fromFDL();

    /** Object that represents the comments and deprecation status of the field */
    public Description getDescription();

    /** return true if this is a scalar value, false it is a list or a struct */
    public boolean isScalar();

    /** returns true if it is a normal output, false if it is a plugin output or an argument */
    public boolean isOutput();

    /**
     * returns true if this field is marked with @per for use in a metric (only applicable to
     * session fields)
     */
    public boolean isPer();

    /* return true if this field is marked with @session_key (only applicable to session fields) */
    public boolean isSessionKey();

    /* return true if this field is marked with @split_key (only applicable to session fields) */
    public boolean isSplitKey();

    /* returns true if this field is the persistent key (only applicable to session fields) */
    public boolean isPersistentKey();

    /** returns the source code location where this entitity was defined */
    public FileLocation getFileLocation();

    /** returns the feature that this was defined on */
    public String getFeatureName();

    public enum ElapsedType {
        SESSION_END
    };

    /** returns the elapsed type (@elapsed directive). null if not an elapsed type */
    public ElapsedType getElapsedType();

}
