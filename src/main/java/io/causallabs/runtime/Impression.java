package io.causallabs.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;

// Plugin visible methods on all impressions
public interface Impression {

  public String getFeatureName();

  // Serialize the result to be sent to the client. Just writes the fields. You must be in an
  // object context
  public void serializeImpression(JsonGenerator gen) throws IOException;

  // return a Json representation of the impression
  public default String toJson() {
    java.io.StringWriter sw = new java.io.StringWriter();
    try {
      JsonGenerator generator = CausalClient.m_mapper.getFactory().createGenerator(sw);
      generator.writeStartObject();
      serializeImpression(generator);
      generator.writeEndObject();
      generator.close();
      return sw.toString();
    } catch (IOException e) {
      // we are writing to a string, so this should never fail
      throw new RuntimeException("Error creating in memory JSON string.", e);
    }
  }
  ;
}
