package io.causallabs.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CausalClientBase {

    protected CausalClientBase(String impressionServerUrl) {
        m_impressionServerUrl = impressionServerUrl;
    }

    public JsonGenerator createGenerator() {
        StringWriter sw = new StringWriter();
        try {
            return m_mapper.getFactory().createGenerator(sw);
        } catch (IOException e) {
            // we are writing to a string, so this should never fail
            throw new RuntimeException("Error creating in memory generator.", e);
        }
    }

    private String getResult(JsonGenerator gen) {
        try {
            StringWriter sw = (StringWriter) (gen.getOutputTarget());
            gen.close();
            return sw.toString();
        } catch (IOException e) {
            // we are writing to a string, so this should never fail
            throw new RuntimeException("Error getting generation result.", e);
        }
    }

    public CompletableFuture<Void> requestAsync(SessionRequestable session, Requestable... requests)
            throws InterruptedException {
        return requestAsync(session, UUID.randomUUID().toString(), requests);
    }

    public CompletableFuture<Void> requestAsync(SessionRequestable session, String impressionId,
            Requestable... requests) throws InterruptedException {
        try {
            JsonGenerator _gen = createGenerator();
            _gen.writeStartObject();
            _gen.writeFieldName("args");
            session.serializeArgs(_gen);
            _gen.writeStringField("impressionId", impressionId);
            return requestAsync(session, _gen, requests);
        } catch (IOException e) {
            // this shouldn't happen because the generator writes to RAM.
            throw new RuntimeException("Error serializing to RAM");
        }
    }

    public void request(SessionRequestable session, Requestable... requests)
            throws InterruptedException {
        request(session, UUID.randomUUID().toString(), requests);
    }

    public void request(SessionRequestable session, String impressionId, Requestable... requests)
            throws InterruptedException {
        CompletableFuture<Void> result = requestAsync(session, impressionId, requests);
        try {
            result.get();
        } catch (ExecutionException e) {
            // this should not happen because all errors from the request are recoverable
            throw new RuntimeException("Unexpected exception.", e);
        }
    }

    // the generator has the device and session args encoded. Encode the rest and
    // execute the
    // request.
    protected CompletableFuture<Void> requestAsync(SessionRequestable session, JsonGenerator gen,
            Requestable... requests) throws InterruptedException {
        for (Requestable req : requests) {
            req.setSession(session);
        }

        try {
            gen.writeFieldName("reqs");
            gen.writeStartArray();

            for (Requestable request : requests) {
                gen.writeStartObject();
                gen.writeStringField("name", request.featureName());
                gen.writeFieldName("args");
                request.serializeArgs(gen);
                gen.writeEndObject();
            }
            gen.writeEndArray();
            gen.writeEndObject();
        } catch (IOException e) {
            logger.error("IO Error creating request, using control", e);
            // this should never happen because we are writing to a string.
            throw new RuntimeException("IO Error creating request, using control", e);
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(m_impressionServerUrl + "/features"))
                .setHeader("user-agent", "Causal java client")
                .header("Content-Type", "application/json").header("Accept", "text/plain")
                .POST(BodyPublishers.ofString(getResult(gen))).build();
        CompletableFuture<HttpResponse<InputStream>> responseFuture =
                m_client.sendAsync(req, BodyHandlers.ofInputStream());
        CompletableFuture<Void> finalFuture = responseFuture.handle(
                (BiFunction<HttpResponse<InputStream>, Throwable, Void>) (resp, exception) -> {

                    if (exception != null) {
                        // Error while connecting to the server
                        errorOutRequests(exception, requests);
                        return null;
                    }
                    if (resp.statusCode() != 200) {
                        // if we get an error code, throw an Api exception
                        try {
                            errorOutRequests(
                                    new ApiException(
                                            "Error code " + resp.statusCode() + " from server: "
                                                    + new String(resp.body().readAllBytes())),
                                    requests);
                        } catch (IOException e) {
                            errorOutRequests(
                                    new ApiException(
                                            "Error code " + resp.statusCode() + " from server"),
                                    requests);
                        }
                        return null;
                    }

                    try {
                        JsonParser parser = m_mapper.getFactory().createParser(resp.body());
                        if (parser.nextToken() != JsonToken.START_OBJECT) {
                            errorOutRequests(new IOException("Malformed response, using control."),
                                    requests);
                            return null;
                        }
                        parser.nextToken();
                        if (parser.getCurrentName().equals("session")) {
                            try {
                                parser.nextToken();
                                session.deserializeResponse(parser);
                            } catch (ApiException e) {
                                errorOutRequests(new ApiException(
                                        "Error parsing response from server for the session, reverting to control."),
                                        requests);
                                return null;
                            }
                        }
                        if (!parser.getCurrentName().equals("impressions")) {
                            errorOutRequests(new IOException(
                                    "Malformed response, expecting 'impressions', using control."),
                                    requests);
                            return null;
                        }

                        if (!JsonToken.START_ARRAY.equals(parser.nextToken())) {
                            errorOutRequests(
                                    new IOException(
                                            "Malformed response, expecting array, using control."),
                                    requests);
                            return null;
                        }
                        parser.nextToken();
                        for (Requestable request : requests) {
                            if (parser.currentToken().equals(JsonToken.END_ARRAY)) {
                                errorOutRequests(
                                        new IOException(
                                                "Response too short, using control values."),
                                        requests);
                                return null;
                            }
                            if (parser.currentToken().equals(JsonToken.VALUE_STRING)) {
                                if (parser.getText().equals("OFF")) {
                                    request.setActive(false);
                                    parser.nextToken();
                                    continue;
                                } else if (parser.getText().equals("UNKNOWN")) {
                                    // server doesn't know about this feature yet
                                    request.setError(new ApiException("Server doesn't know feature "
                                            + request.featureName() + ", using control."));
                                    logger.info(request.getError().getMessage());
                                    parser.nextToken();
                                    continue;
                                }
                            }
                            if (!parser.currentToken().equals(JsonToken.START_OBJECT)) {
                                request.setError(new ApiException("Malformed response for "
                                        + request.featureName() + ", using control values."));
                                logger.warn(request.getError().getMessage());;
                                consumeValue(parser);
                            }
                            try {
                                request.deserializeResponse(parser);
                            } catch (ApiException e) {
                                request.setError(
                                        new ApiException("Error parsing response from server for "
                                                + request.featureName()
                                                + ", reverting to control."));
                                logger.warn(request.getError().getMessage());;
                            }
                        }
                    } catch (JsonParseException e1) {
                        errorOutRequests(new ApiException("Malformed response, using control.", e1),
                                requests);
                    } catch (IOException e1) {
                        // may happen if we lose connection mid string
                        errorOutRequests(new ApiException("Malformed response, using control.", e1),
                                requests);
                    }
                    return null;
                });

        // wait for the result, cause if we dont, then the process may terminate before
        // the impression registers
        m_threadPool.submit(() -> {
            try {
                finalFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return finalFuture;
    }

    // mark the requests with the recoverable error and log it.
    private void errorOutRequests(Throwable exception, Requestable[] requests) {
        logger.warn(exception.getMessage());
        for (Requestable r : requests) {
            r.setError(exception);
        }
        return;
    }

    // skip over the value that the parser is currently pointing to
    public static void consumeValue(JsonParser parser) throws IOException {
        switch (parser.currentToken()) {
            case START_ARRAY:
            case START_OBJECT:
                parser.skipChildren();
                parser.nextToken();
                break;
            default:
                parser.nextToken();
        }
    }

    // Send the Json payload to the signal handler (asynchronously)
    public void signal(JsonGenerator gen) {
        final HttpRequest req =
                HttpRequest.newBuilder(URI.create(m_impressionServerUrl + "/signal"))
                        .setHeader("user-agent", "Causal java client")
                        .header("Content-Type", "application/json").header("Accept", "text/plain")
                        .POST(BodyPublishers.ofString(getResult(gen))).build();

        // the threads are not deamons, so they should finish the signaling before terminating the
        // process
        m_threadPool.submit(() -> {
            try {
                // note, we switched away from sendAsync because that had issues with runaway thread
                // allocation
                HttpResponse<String> resp = m_client.send(req, BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    String body = new String(resp.body().getBytes());
                    logger.error("Error signaling impression server: " + body);
                }
            } catch (Exception e) {
                logger.error("Error signaling impression server: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Make a jackson generator to serialize an external callback. The generator is left at the
     * place where you write the external value. caller should call this, write the value to the
     * generator, then call signalExternal
     * 
     * @param deviceId
     * @param impressionIds
     * @param fieldName
     * @return
     */
    public JsonGenerator externalGenerator(SessionRequestable session, List<String> impressionIds,
            String featureName, String fieldName) {
        StringWriter sw = new StringWriter();
        try {
            JsonGenerator gen = m_mapper.getFactory().createGenerator(sw);
            gen.writeStartObject();
            gen.writeFieldName("id");
            session.serializeIds(gen);
            gen.writeObjectField("feature", featureName);
            if (impressionIds.size() > 0)
                gen.writeObjectField("impressionId", impressionIds.get(0));
            gen.writeFieldName(fieldName);
            return gen;
        } catch (IOException e) {
            // we are writing to a string, so this should never fail
            throw new RuntimeException("Error creating in memory generator.", e);
        }
    }

    /**
     * See externalGenerator
     * 
     * @param gen
     */
    public void signalExternal(JsonGenerator gen) {
        try {
            gen.writeEndObject();
            HttpRequest req = HttpRequest
                    .newBuilder(URI.create(m_impressionServerUrl + "/external"))
                    .setHeader("user-agent", "Causal java client")
                    .header("Content-Type", "application/json").header("Accept", "text/plain")
                    .POST(BodyPublishers.ofString(getResult(gen))).build();
            CompletableFuture<Void> future =
                    m_client.sendAsync(req, BodyHandlers.ofString()).thenAcceptAsync(resp -> {
                        if (resp.statusCode() != 200) {
                            String body = new String(resp.body().getBytes());
                            logger.error("Error writing external: " + body);
                            throw new RuntimeException("Error writing external: " + body);
                        }
                    });
            m_threadPool.submit(() -> {
                try {
                    // wait for the result, cause if we dont, then the process may terminate before
                    // the signal is sent
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            // we are writing to a string, so this should never fail
            throw new RuntimeException("Error creating in memory generator.", e);
        }
    }

    // need a daemon thread pool to wait for the asynchronous operations because the
    // process may exit before completing them otherwise
    private static ExecutorService m_threadPool =
            Executors.newFixedThreadPool(4, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }

            });
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    m_threadPool.shutdown();
                    m_threadPool.awaitTermination(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String m_impressionServerUrl;
    HttpClient m_client = HttpClient.newHttpClient();
    public static final Logger logger = LoggerFactory.getLogger(CausalClientBase.class);
    public static final ObjectMapper m_mapper = new ObjectMapper();
}
