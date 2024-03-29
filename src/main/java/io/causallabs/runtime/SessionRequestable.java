package io.causallabs.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;

/** Marker interface for the session request objects */
public abstract class SessionRequestable {
  // serialize the arguments to be sent to the impression server. outputs a map with all the
  // arguments that are set
  public abstract void serializeArgs(JsonGenerator gen);

  // serialize the session identifiers to be sent to the impression server.
  // outputs them as an object
  public abstract void serializeIds(JsonGenerator gen);

  // Take a response from the server and deserialize it into this object.
  // ApiExceptions can happen if we try to deserialize a value to somewhere
  // it can't go. IE deserializing a string to an int.
  public abstract void deserializeResponse(JsonParser parser) throws ApiException;

  // Add appropriate headers to send a request to the impression server
  public abstract void addHeaders(SimpleRequestBuilder builder);

  public void setComplete() {
    _callComplete = true;
  }

  public void checkComplete() {
    if (!_callComplete)
      throw new IllegalStateException(
          "Attempt to access a request before calling a CausalClient.request method.");
  }

  private boolean _callComplete = false;
}
