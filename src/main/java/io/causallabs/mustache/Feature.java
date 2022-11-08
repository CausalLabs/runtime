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
     * return all the fields that are in the warehouse table for this feature
     * 
     * @return
     */
    public Collection<WarehouseColumn> getWarehouseColumns();

}
