package io.causallabs.mustache;

import java.util.List;

/** Represents the definition of an abstract event, inherited by other events in the warehouse */
public interface AbstractEvent extends Event {

  /** Return the name reserved for the event view in the data warehouse */
  public String getEventViewName();

  /**
   * Return all the places in the warehouse where events derived from this are stored.
   *
   * @return
   */
  public List<FeatureColumn> getDerivedColumns();
}
