/**
 * This package defines the context made available to the JMustache template processor when
 * expanding user-defined mustache template with the --template causalc command line option.
 * <p>
 * The context includes: <ul>
 * <li>session - points to the session {@link io.causallabs.mustache.Feature}</li>
 * <li>features - points to the list of {@link io.causallabs.mustache.Feature}s</li>
 * <li>events - points to the list of abstract {@link io.causallabs.mustache.AbstractEvent}s</li>
 * </ul>
 * <p>
 * In addition the values stored in the json file specified using the --template-params argument. So
 * if you have a params file as follows:
 * 
 * <pre>
 *  {"hello" : "world"}
 * </pre>
 * 
 * Then in the mustache file {{params.hello}} will expand to "world".
 * 
 */
package io.causallabs.mustache;
