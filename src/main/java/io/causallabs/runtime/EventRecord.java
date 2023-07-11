package io.causallabs.runtime;

import org.apache.avro.generic.IndexedRecord;

/**
 * This interface is used internally by Causal. It includes the base functionality we need to
 * represent a event being signalled on the impression server
 */
public interface EventRecord extends IndexedRecord, ImpressionEvent {

  /** Index of the event inside a context, or -1 if offline */
  public int getIndex();

  /**
   * A unique index for this event type for all events that can occur within a session. Used to
   * quickly calculate audience membership
   */
  public int getGlobalIndex();
}
