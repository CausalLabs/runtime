package io.causallabs.mustache;

import java.util.Collection;
import java.util.Map;

/** This is the top level context provided to the template by the compiler */
public interface Context {

    /** The session definition */
    public Feature getSession();

    /** All the non-abstract feature definitions */
    public Collection<Feature> getFeatures();

    /** All the abstract event definitions */
    public Collection<AbstractEvent> getEvents();

    /**
     * the set of parameters provided to the compiler using the --template-params command line
     * option
     */
    public Map<String, Object> getParams();
}
