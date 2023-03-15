package io.causallabs.runtime;

import org.apache.avro.generic.IndexedRecord;

/**
 * This interface is used internally by Causal. It includes the base functionality we need to
 * represent a event being signalled on the impression server
 */
public interface EventRecord extends IndexedRecord, ImpressionEvent {

  public int getIndex();

  public int getGlobalIndex();
}
