package io.causallabs.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface that must be supported for java classes that represent a feature inside the impression
 * server
 */
public interface Requested {

    /** reset everything to default values */
    public void reset();

    // Return true if the request should be memoized with the previous request
    public boolean argsMatch(Requested obj);

    // Return true if the @session_key matches. If not, we need to create a new session
    public boolean keysMatch(Requested obj);

    // Increment the count if another request was just memoized with this one.
    // optionally pass a new impressionId to keep track of
    public void incCount(String impressionId);

    public long count();

    public String featureName();

    //////////////////////////////////////////////////////////////////////////////////
    // Serialization Support
    //////////////////////////////////////////////////////////////////////////////////

    // Deserialize the arguments from a client into this object
    public void deserializeArgs(JsonParser next) throws ApiException;

    // Serialize the result to be sent to the client. Just writes the fields. You must be in an
    // object context
    public void serializeResponse(JsonGenerator gen);

    //////////////////////////////////////////////////////////////////////////////////
    // avro style access to fields
    //////////////////////////////////////////////////////////////////////////////////

    public void put(int i, Object o);

    public boolean isSet(int i);

    // Return user visible representation of the field
    public Object get(int i);

    // does not translate objects from their internal representation.
    // used to log data to the avro file
    // currently just returns a Mutable for mutable fields.
    public Object getLog(int i);

    // used to set a new value on a mutable field
    public void putMutation(int i, Object o);

    // set the given output as an external value with the given name
    public void putExternal(int i, String name);

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
