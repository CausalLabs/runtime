package io.causallabs.mustache;

import java.util.Collection;

/** Represents a context in FDL. That is a Feature or Session, along with its fields and events. */
public interface Feature {
  /** the name of the feature. "session" if this is the session context */
  public String getName();

  /**
   * The name of the feature's table in the warehouse. Basically the name, but in snake case to
   * support SQLs case insensitivity.
   */
  public String getWarehouseTableName();

  /** All the fields that are in the warehouse table for this feature */
  public Collection<FeatureColumn> getWarehouseColumns();

  /** The events that are declared inside this feature */
  public Collection<FeatureEvent> getFeatureEvents();

  /** The source code location where this feature was defined */
  public FileLocation getFileLocation();

  /**
   * The attributes of this feature marked with @per (i.e. are available to use as a per in a
   * metric)
   */
  public Collection<FeatureColumn> getPerColumns();

  /** The attributes of this feature that are marked @session_key. Only applicable to sessions */
  public Collection<FeatureColumn> getSessionKeys();

  /**
   * The attributes of this feature that are marked @split_key. Only applicable to sessions.
   * Currently limited to one key
   */
  public Collection<FeatureColumn> getSplitKeys();

  /**
   * The attribute of this feature that is used as the persistent key. Only applicable to sessions
   */
  public FeatureColumn getPersistentKey();

  /** The attributes of this feature that are defined with @elapsed */
  public Collection<FeatureColumn> getElapsedColumns();
}
