package io.causallabs.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.StringWriter;

/** Plugin visible methods for all events */
public interface ImpressionEvent {

  public String getEventName();

  public long getEventTime();

  /** Returns null for non impression based events */
  public String getImpressionId();

  /** returns null if not an offline event */
  public String getSplitKey();

  // Serialize the result to be sent to the client. Just writes the fields. You must be in an
  // object context
  public void serializeEvent(JsonGenerator gen) throws IOException;

  /**
   * return a Json representation of the event
   *
   * @return
   */
  public default String toJson() {
    StringWriter sw = new StringWriter();
    try {
      JsonGenerator _gen = CausalClient.m_mapper.getFactory().createGenerator(sw);
      _gen.writeStartObject();
      serializeEvent(_gen);
      _gen.writeEndObject();
      _gen.close();
      return sw.toString();
    } catch (IOException e) {
      // we are writing to a string, so this should never fail
      throw new RuntimeException("Error creating in memory generator.", e);
    }
  }
}
