package io.causallabs.runtime;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Marker interface for the session request objects
 */
public abstract class SessionRequestable {
    // serialize the arguments to be sent to the impression server. outputs a map with all the
    // arguments that are set
    public abstract void serializeArgs(JsonGenerator gen);

    // serialize the session identifiers to be sent to the impression server.
    // outputs them as an object
    public abstract void serializeIds(JsonGenerator gen);
}
