package io.causallabs.runtime;

import com.fasterxml.jackson.core.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface that must be supported for java classes that represent a feature inside the impression
 * server
 */
public interface Requested extends Impression {

  /** reset everything to default values */
  public void reset();

  // Return true if the request should be memoized with the previous request
  public boolean argsMatch(Requested obj);

  // Return true if the @session_key matches. If not, we need to create a new session
  public boolean keysMatch(Requested obj);

  // Increment the count if another request was just memoized with this one.
  // optionally pass a new impressionId to keep track of
  @Deprecated
  public void incCount(String impressionId);

  public void addImpression(String impressionId, long timestamp);
  // return true if the memoized request contains the given impression id
  public boolean hasImpression(String impressionId);
  // return the last (and current) impression ID for this impression
  public String lastImpressionId();

  public long count();

  //////////////////////////////////////////////////////////////////////////////////
  // Serialization Support
  //////////////////////////////////////////////////////////////////////////////////

  // Deserialize the arguments from a client into this object
  public void deserializeArgs(JsonParser next) throws ApiException;

  //////////////////////////////////////////////////////////////////////////////////
  // avro style access to fields
  //////////////////////////////////////////////////////////////////////////////////

  public void put(int i, Object o);

  public boolean isSet(int i);

  // Return user visible representation of the field
  public Object get(int i);

  // set the given output as an external value with the given name
  // also marks the field as unset
  public void putExternal(int i, String name);

  // if the output is set externally, the name of the external value type
  // null otherwise
  public String getExternal(int i);

  ////////////////////////////////////////////////////////////////////////////////
  // Support for feature gating
  ////////////////////////////////////////////////////////////////////////////////
  public void setActive(boolean b);

  /**
   * Is this feature active?
   *
   * @return false if gated off
   */
  public boolean isActive();

  public static final Logger logger = LoggerFactory.getLogger(Requested.class);
}
