package io.causallabs.runtime;

import org.apache.avro.generic.IndexedRecord;

/**
 * This class is used internally by Causal. It includes the base functionality we need to represent
 * a event being signalled on the impression server
 */
public interface EventRecord extends IndexedRecord {

    public void setImpressionId(String x);

    public String getImpressionId();

    public void setEventTime(long x);

    public long getEventTime();

    public int getIndex();

    public int getGlobalIndex();

    public String getEventName();

}
