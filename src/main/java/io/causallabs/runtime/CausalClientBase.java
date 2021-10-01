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
import java.util.concurrent.CompletableFuture;
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

    // the generator has the device and session args encoded. Encode the rest and execute the
    // request.
    protected CompletableFuture<Void> requestAsync(JsonGenerator gen, String deviceId,
            Requestable... requests) throws InterruptedException {

        try {
            gen.writeFieldName("reqs");
            gen.writeStartArray();
            for (Requestable request : requests) {
                request.setDeviceId(deviceId);
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
        return responseFuture.handle(
                (BiFunction<HttpResponse<InputStream>, Throwable, Void>) (resp, exception) -> {

                    if (exception != null) {
                        // Error while connecting to the server
                        errorOutRequests(exception, requests);
                        return null;
                    }
                    if (resp.statusCode() != 200) {
                        // if we get an error code, throw an Api exception
                        errorOutRequests(new ApiException("Error code " + resp.statusCode()
                                + " from server: " + resp.body().toString()), requests);
                        return null;
                    }

                    try {
                        JsonParser parser = m_mapper.getFactory().createParser(resp.body());
                        if (!JsonToken.START_ARRAY.equals(parser.nextToken())) {
                            errorOutRequests(new IOException("Malformed response, using control."),
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
    }

    // mark the requests with the recoverable error and log it.
    private void errorOutRequests(Throwable exception, Requestable[] requests) {
        logger.warn(exception.getMessage(), exception);
        for (Requestable r : requests) {
            r.setError(exception);
        }
        return;
    }

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
        HttpRequest req = HttpRequest.newBuilder(URI.create(m_impressionServerUrl + "/signal"))
                .setHeader("user-agent", "Causal java client")
                .header("Content-Type", "application/json").header("Accept", "text/plain")
                .POST(BodyPublishers.ofString(getResult(gen))).build();
        m_client.sendAsync(req, BodyHandlers.ofString()).thenAccept(resp -> {
            if (resp.statusCode() != 200) {
                logger.error("Error signaling event: " + resp);
                throw new RuntimeException("Error signaling event: " + resp);
            }
        });
    }

    private String m_impressionServerUrl;
    HttpClient m_client = HttpClient.newHttpClient();
    public static final Logger logger = LoggerFactory.getLogger(CausalClientBase.class);
    public static final ObjectMapper m_mapper = new ObjectMapper();
}

