package io.causallabs.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Interface that must be supported for java classes that represent a feature in the Java client API
 */
public interface Requestable {

    public String featureName();

    ////////////////////////////////////////////////////////////////////////////////
    // Support for feature gating
    ////////////////////////////////////////////////////////////////////////////////
    /**
     * Is this feature active?
     * 
     * @return false if gated off
     */
    public boolean isActive();

    ////////////////////////////////////////////////////////////////////////////////
    // Support for recoveralbe errors
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if a recoverable error occurred
     * 
     * @return If the request resulted in a recoverable error, the error, else null.
     */
    public Throwable getError();

    public String getDeviceId();

    public void setDeviceId(String x);

    ////////////////////////////////////////////////////////////////////////////////
    // For use by causal runtinme
    ////////////////////////////////////////////////////////////////////////////////
    void setActive(boolean b);

    void setError(Throwable t);

    // serialize the arguments to be sent to the impression server. gen should
    // already be in the object context
    void serializeArgs(JsonGenerator gen);

    // Take a response from the server and deserialize it into this object.
    // ApiExceptions can happen if we try to deserialize a value to somewhere
    // it can't go. IE deserializing a string to an int.
    void deserializeResponse(JsonParser parser) throws ApiException;
}
