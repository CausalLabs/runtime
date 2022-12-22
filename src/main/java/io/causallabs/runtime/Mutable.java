package io.causallabs.runtime;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class Mutable<T> {

    public Mutable() {}

    public Mutable(T initialValue) {
        setInitialValue(initialValue);
    }

    public void reset() {
        values.clear();
    }

    public void setInitialValue(T x) {
        // we could make this a bunch more memory efficient if we need to - TODO
        reset();
        values.add(new Record<T>(Mutable.getClock().millis(), x));
    }

    public void setValue(T x) {
        long now = Mutable.getClock().millis();
        if (!values.isEmpty()) {
            values.get(values.size() - 1).endTime = now - 1;
        }
        values.add(new Record<T>(now, x));
    }

    public boolean isSet() {
        return !values.isEmpty();
    }

    public T getValue() {
        return values.get(values.size() - 1).value;
    }

    public List<Record<T>> getHistory() {
        if (!values.isEmpty()) {
            values.get(values.size() - 1).endTime = Mutable.getClock().millis() - 1;
        }
        return values;
    }

    public static class Record<T> implements IndexedRecord {
        public Record(long now, T x) {
            startTime = now;
            value = x;
        }

        long startTime, endTime;
        T value;

        @Override
        public Schema getSchema() {
            // schema is generated in the iserver
            return null;
        }

        @Override
        public void put(int i, Object v) {
            // we don't read these in
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0:
                    return startTime;
                case 1:
                    return endTime;
                case 2:
                    return value;
            }
            return null;
        }
    }

    ArrayList<Record<T>> values = new ArrayList<>();

    public static Clock getClock() {
        return m_clock;
    }

    // we allow swapping out the clocks for integration testing.
    // do not use this
    public static void setClock(Clock clock) {
        logger.warn("Replacing system defined clock");
        m_clock = clock;
    }

    // here so it can be manipulated for integration tests.
    private static Clock m_clock = Clock.systemUTC();
    private static Logger logger = LoggerFactory.getLogger(Mutable.class);

}
