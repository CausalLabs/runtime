package io.causallabs.mustache;

import java.util.List;

public interface Event {

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


}
