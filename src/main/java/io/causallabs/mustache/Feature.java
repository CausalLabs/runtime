package io.causallabs.mustache;

import java.util.Collection;

/**
 * Represents a context in FDL. That is a Feature or Session, along with its fields and events.
 */
public interface Feature {
    /**
     * the name of the feature. "session" if this is the session context
     */
    public String getName();

    /**
     * The name of the feature's table in the warehouse. Basically the name, but in snake case to
     * support SQLs case insensitivity.
     */
    public String getWarehouseTableName();

    /**
     * All the fields that are in the warehouse table for this feature
     */
    public Collection<WarehouseColumn> getWarehouseColumns();

    /**
     * The events that are declared inside this feature
     */
    public Collection<FeatureEvent> getFeatureEvents();

    /**
     * The file location where this feature was defined
     */
    public FileLocation getFileLocation();

    // /**
    // * The attributes of this feature marked with @per (i.e. are available to use as a per in a
    // * metric)
    // */
    // public Collection<WarehouseColumn> getPerColumns();

    // interface ElapsedDirective {
    // enum ElapsedType {
    // SESSION_END
    // };
    // }

    // /**
    // * The attributes of this feature that are defined with @elapsed
    // */
    // public Collection<ElapsedDirective> getElapsedColumns();

}
