package io.causallabs.mustache;

import java.util.List;

/** Represents the definition of an abstract event, inherited by other events in the warehouse */
public interface Event {

    /** Return the name reserved for the event view in the data warehouse */
    public String getEventViewName();

    public interface Column {
        public String getColumnName();

        public String getOrcType();

        public String getRedshiftType();

        public Description getDescription();

        /** return true if this is a scalar value. False it is a list or a struct */
        public boolean isScalar();

    }

    /**
     * When the event is exposed in a view, the names and data types of the columns in that table.
     */
    public List<Column> getEventColumns();

    /**
     * Return all the places in the warehouse where events derived from this are stored.
     * 
     * @return
     */
    public List<WarehouseColumn> getDerivedColumns();
}
