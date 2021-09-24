package io.causallabs.runtime;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Interface that must be supported for java classes that represent a feature or session impression
 */
public interface Requestable {

    /**
     * Memoization support. Return true if the request should be memoized with the previous request
     */
    public boolean argsMatch(Requestable obj);

    /**
     * Memoization support. Increment the count if another request was just memoized with this one.
     */
    public void incCount();

    public long count();

    public String featureName();

    /**
     * serialize the arguments to be sent to the impression server. gen should already be in the
     * object context
     */
    public void serializeArgs(JsonGenerator gen) throws IOException;

    /**
     * ApiExceptions can happen if we try to deserialize a value to somewhere it can't go. IE
     * deserializing a string to an int.
     * 
     * @param next
     * @throws ApiException
     */
    public void deserializeArgs(JsonParser next) throws ApiException;

    /** serialize the result to be sent to the client */
    public void serializeResponse(JsonGenerator gen) throws IOException;

    /**
     * ApiExceptions can happen if we try to deserialize a value to somewhere it can't go. IE
     * deserializing a string to an int.
     * 
     * @param next
     * @throws ApiException
     */
    public void deserializeResponse(JsonParser parser) throws ApiException;

    /** avro style access to fields */
    public void put(int i, Object o);

    public boolean isSet(int i);

    public Object get(int i);

    /** reset everything to default values */
    public void reset();

    /** Support for feature gating */
    public void setActive(boolean b);

    public boolean isActive();

    public String getDeviceId();

    public void setDeviceId(String x);

}
