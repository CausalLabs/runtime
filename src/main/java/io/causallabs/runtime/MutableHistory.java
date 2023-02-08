package io.causallabs.runtime;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Objects;

/** Class that represents the changing value of a mutable over time. */
public class MutableHistory<T> extends ArrayList<MutableHistory.Record<T>> {

    public MutableHistory() {}

    public void setValue(T x) {
        long now = MutableHistory.getClock().millis();
        if (!isEmpty()) {
            Record<T> last = get(size() - 1);
            if (Objects.equals(last.value, x))
                return;
            last.endTime = now;
        }
        add(new Record<T>(now, x));
    }

    public boolean isSet() {
        return !isEmpty();
    }

    public T getValue() {
        return get(size() - 1).value;
    }

    public static class Record<T> implements IndexedRecord {
        public Record(long now, T x) {
            startTime = now;
            value = x;
            endTime = Long.MAX_VALUE;
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
    private static Logger logger = LoggerFactory.getLogger(MutableHistory.class);

}
