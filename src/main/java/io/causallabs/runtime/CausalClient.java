package io.causallabs.runtime;

import java.io.IOException;
import java.io.StringWriter;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CausalClient {

    public static synchronized CausalClient init(String impressionServerURL) {
        if (m_instance != null) {
            throw new IllegalStateException("Causal client was already initialized");
        }
        m_instance = new CausalClient(impressionServerURL);
        return m_instance;
    }

    public static synchronized CausalClient getInstance() {
        if (m_instance == null) {
            // this is for back compatibility
            String url;
            if (System.getenv("CAUSAL_ISERVER") != null)
                url = System.getenv("CAUSAL_ISERVER");
            else if (System.getProperty("io.causallabs.iserverUrl") != null) {
                url = System.getProperty("io.causallabs.iserverUrl");
            } else {
                logger.warn("CAUSAL_ISERVER not set. Using http://localhost:3004/iserver");
                url = "http://localhost:3004/iserver";
            }
            m_instance = new CausalClient(url);
        }
        return m_instance;
    }

    private CausalClient(String impressionServerURL) {
        m_impressionServerUrl = impressionServerURL;
        m_asyncClient = HttpAsyncClients.createDefault();
        m_asyncClient.start();
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
            Requestable... requests) {
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
            throws InterruptedException, ApiException {
        request(session, UUID.randomUUID().toString(), requests);
    }

    public void request(SessionRequestable session, String impressionId, Requestable... requests)
            throws InterruptedException, ApiException {
        JsonGenerator _gen = createGenerator();
        try {
            _gen.writeStartObject();
            _gen.writeFieldName("args");
            session.serializeArgs(_gen);
            _gen.writeStringField("impressionId", impressionId);
        } catch (IOException e) {
            // this shouldn't happen because the generator writes to RAM.
            throw new RuntimeException("Error serializing to RAM");
        }
        request(session, _gen, requests);
    }

    private void setupRequest(SessionRequestable session, JsonGenerator gen,
            Requestable[] requests) {
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

    }

    private void handleResponse(SimpleHttpResponse resp, SessionRequestable session,
            JsonGenerator gen, Requestable[] requests) throws ApiException {
        if (resp.getCode() != 200) {
            // if we get an error code, throw an Api exception
            ApiException exception = new ApiException(resp.getCode(),
                    "Error code " + resp.getCode() + " from server: " + resp.getBodyText());
            errorOutRequests(exception, requests);
            throw exception;
        }

        try {
            JsonParser parser = m_mapper.getFactory().createParser(resp.getBodyText());
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                ApiException exception =
                        new ApiException(500, "Malformed response, using control.");
                errorOutRequests(exception, requests);
                throw exception;
            }
            parser.nextToken();
            if (parser.getCurrentName().equals("session")) {
                try {
                    parser.nextToken();
                    session.deserializeResponse(parser);
                } catch (ApiException e) {
                    errorOutRequests(e, requests);
                    throw e;
                }
            }
            if (!parser.getCurrentName().equals("impressions")) {
                ApiException exception = new ApiException(500,
                        "Malformed response, expecting 'impressions', using control.");
                errorOutRequests(exception, requests);
                throw exception;
            }

            if (!JsonToken.START_ARRAY.equals(parser.nextToken())) {
                ApiException exception = new ApiException(500,
                        "Malformed response, expecting array, using control.");
                errorOutRequests(exception, requests);
                throw exception;
            }
            parser.nextToken();
            ApiException delayedException = null;
            for (Requestable request : requests) {
                if (parser.currentToken().equals(JsonToken.END_ARRAY)) {
                    ApiException exception =
                            new ApiException(500, "Response too short, using control values.");
                    errorOutRequests(exception, requests);
                    throw exception;
                }
                if (parser.currentToken().equals(JsonToken.VALUE_STRING)) {
                    if (parser.getText().equals("OFF")) {
                        request.setActive(false);
                        parser.nextToken();
                        continue;
                    } else if (parser.getText().equals("UNKNOWN")) {
                        // server doesn't know about this feature yet, this
                        // error is expected during schema migration, so
                        // shouldn't throw an exception
                        request.setError(new ApiException("Server doesn't know feature "
                                + request.featureName() + ", using control."));
                        logger.info(request.getError().getMessage());
                        parser.nextToken();
                        continue;
                    }
                }
                if (!parser.currentToken().equals(JsonToken.START_OBJECT)) {
                    delayedException = new ApiException(500, "Malformed response for "
                            + request.featureName() + ", using control values.");
                    request.setError(delayedException);
                    logger.warn(request.getError().getMessage());;
                    consumeValue(parser);
                }
                try {
                    request.deserializeResponse(parser);
                } catch (ApiException e) {
                    delayedException =
                            new ApiException(500, "Error parsing response from server for "
                                    + request.featureName() + ", reverting to control.", e);
                    request.setError(delayedException);
                    logger.warn(request.getError().getMessage());;
                }
            }
            if (parser.nextToken() == JsonToken.FIELD_NAME) {
                if (parser.currentName().equals("errors")) {
                    // handle any errors from the request
                    if (!JsonToken.START_ARRAY.equals(parser.nextToken())) {
                        ApiException exception = new ApiException(500,
                                "Malformed response, expecting array. May be unreported errors.");
                        errorOutRequests(exception, requests);
                        throw exception;
                    }
                    int index = 0;
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.VALUE_NULL)
                            index++;
                        else {
                            delayedException = new ApiException(500, parser.getText());
                            requests[index++].setError(delayedException);
                        }
                    }
                }
            }
            if (delayedException != null)
                throw delayedException;
        } catch (JsonParseException e1) {
            ApiException exception =
                    new ApiException(500, "Malformed response, using control.", e1);
            errorOutRequests(exception, requests);
            throw exception;
        } catch (IOException e1) {
            // may happen if we lose connection mid string
            errorOutRequests(e1, requests);
            throw new ApiException(500, "Error reading from server", e1);
        }
    }

    protected void request(SessionRequestable session, JsonGenerator gen, Requestable... requests)
            throws InterruptedException, ApiException {
        CompletableFuture<Void> result = requestAsync(session, gen, requests);
        try {
            result.get();
        } catch (ExecutionException e) {
            throw (ApiException) e.getCause();
        }
    }

    // the generator has the device and session args encoded. Encode the rest and
    // execute the
    // request.
    protected CompletableFuture<Void> requestAsync(SessionRequestable session, JsonGenerator gen,
            Requestable... requests) {


        setupRequest(session, gen, requests);
        CompletableFuture<Void> result = new CompletableFuture<>();
        asyncSendJson(URI.create(m_impressionServerUrl + "/features"), getResult(gen),
                new FutureCallback<SimpleHttpResponse>() {

                    @Override
                    public void completed(SimpleHttpResponse resp) {

                        try {
                            handleResponse(resp, session, gen, requests);
                            result.complete(null);
                        } catch (ApiException e2) {
                            result.completeExceptionally(e2);
                        }
                    }

                    @Override
                    public void failed(Exception exception) {
                        // Error while connecting to the server
                        errorOutRequests(exception, requests);
                        result.completeExceptionally(exception);
                    }

                    @Override
                    public void cancelled() {
                        result.completeExceptionally(new InterruptedException());
                    }

                });

        return result;
    }

    // mark the requests with the recoverable error and log it.
    private void errorOutRequests(Exception exception, Requestable[] requests) {
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
        asyncSendJson("signalling event", URI.create(m_impressionServerUrl + "/signal"),
                getResult(gen));
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
        } catch (IOException e) {
            // should never happen
            logger.error("Error serializing to ram, dropping request", e);
        }
        asyncSendJson("writing external", URI.create(m_impressionServerUrl + "/external"),
                getResult(gen));
    }

    private void asyncSendJson(String what, URI uri, String body) {
        asyncSendJson(uri, body, new FutureCallback<SimpleHttpResponse>() {

            @Override
            public void completed(SimpleHttpResponse result) {
                if (result.getCode() != 200) {
                    logger.error(
                            "Error " + result.getCode() + " " + what + ": " + result.getBodyText());
                }
            }

            @Override
            public void failed(Exception ex) {
                logger.error("Error " + what + ": " + ex.getMessage(), ex);
            }

            @Override
            public void cancelled() {
                logger.error("Request cancelled " + what);
            }
        });
    }

    private void asyncSendJson(URI uri, String body, FutureCallback<SimpleHttpResponse> handler) {
        SimpleHttpRequest reqest =
                SimpleRequestBuilder.post(uri).setBody(body, ContentType.APPLICATION_JSON)
                        .setHeader("user-agent", "Causal java client")
                        .addHeader("Accept", "text/plain").build();

        Future<SimpleHttpResponse> future = m_asyncClient.execute(
                SimpleRequestProducer.create(reqest), SimpleResponseConsumer.create(), handler);

        m_threadPool.submit(() -> {
            try {
                // wait for the result, cause if we dont, then the process may terminate before
                // the signal is sent
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

    private static CausalClient m_instance = null;
    private String m_impressionServerUrl;
    private final CloseableHttpAsyncClient m_asyncClient;
    public static final Logger logger = LoggerFactory.getLogger(CausalClient.class);
    public static final ObjectMapper m_mapper = new ObjectMapper();
}
