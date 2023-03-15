package io.causallabs.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Interface that must be supported for java classes that represent a feature in the Java client API
 */
public abstract class Requestable {

  public abstract String featureName();

  /**
   * Is this feature active?
   *
   * @return false if gated off
   */
  public boolean isActive() {
    return _active;
  }

  /**
   * Check if a recoverable error occurred
   *
   * @return If the request resulted in a recoverable error, the error, else null.
   */
  public Exception getError() {
    return throwable;
  }
  ;

  public final SessionRequestable getSession() {
    return m_session;
  }

  ////////////////////////////////////////////////////////////////////////////////
  // For use by causal runtime and impression server
  ////////////////////////////////////////////////////////////////////////////////

  protected void setActive(boolean x) {
    _active = x;
  }

  private boolean _active = false;

  // recoverable error support
  public void setError(Exception t) {
    _callComplete = true;
    throwable = t;
  }
  ;

  // used to set the defaults when the server doesn't know about the feature
  public void setDefaults() {
    _callComplete = true;
  }
  ;

  public void setComplete() {
    _callComplete = true;
  }

  public void checkComplete() {
    if (!_callComplete)
      throw new IllegalStateException(
          "Attempt to access a request before calling a CausalClient.request method.");
    if (!_active) throw new IllegalStateException("Attempt to access a request that is inactive.");
  }

  private boolean _callComplete = false;
  private Exception throwable = null;

  // serialize the arguments to be sent to the impression server. gen should
  // already be in the object context
  public abstract void serializeArgs(JsonGenerator gen);

  // Take a response from the server and deserialize it into this object.
  // ApiExceptions can happen if we try to deserialize a value to somewhere
  // it can't go. IE deserializing a string to an int.
  public abstract void deserializeResponse(JsonParser parser) throws ApiException;

  void setSession(SessionRequestable s) {
    m_session = s;
  }

  SessionRequestable m_session = null;
}
