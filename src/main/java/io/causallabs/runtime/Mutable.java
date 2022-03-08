package io.causallabs.runtime;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import java.util.ArrayList;
import java.util.List;

public class Mutable<T> {

    public Mutable() {}

    public Mutable(T initialValue) {
        setInitialValue(initialValue);
    }

    public void setInitialValue(T x) {
        // we could make this a bunch more memory efficient if we need to - TODO
        values.clear();
        values.add(new Record<T>(System.currentTimeMillis(), x));
    }

    public void setValue(T x) {
        long now = System.currentTimeMillis();
        if (!values.isEmpty()) {
            values.get(values.size() - 1).endTime = now - 1;
        }
        values.add(new Record<T>(now, x));
    }

    public T getValue() {
        return values.get(values.size() - 1).value;
    }

    public List<Record<T>> getHistory() {
        if (!values.isEmpty()) {
            values.get(values.size() - 1).endTime = System.currentTimeMillis() - 1;
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
}
