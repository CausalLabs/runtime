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
import com.fasterxml.jackson.core.JsonGenerator;
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
            throw new RuntimeException("Can't happen error.", e);
        }
    }

    private String getResult(JsonGenerator gen) {
        try {
            StringWriter sw = (StringWriter) (gen.getOutputTarget());
            gen.close();
            return sw.toString();
        } catch (IOException e) {
            // we are writing to a string, so this should never fail
            throw new RuntimeException("Can't happen error.", e);
        }
    }


    // the generator has the device and session args encoded. Encode the rest and execute the
    // request.
    protected void request(JsonGenerator gen, String deviceId, Requestable... requests)
            throws InterruptedException {

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
            // this should never happen because we are writing to a string.
            logger.error("IO Error creating request, using control", e);
            return;
        }
        try {
            HttpRequest req = HttpRequest
                    .newBuilder(URI.create(m_impressionServerUrl + "/features"))
                    .setHeader("user-agent", "Causal java client")
                    .header("Content-Type", "application/json").header("Accept", "text/plain")
                    .POST(BodyPublishers.ofString(getResult(gen))).build();
            HttpResponse<InputStream> resp = m_client.send(req, BodyHandlers.ofInputStream());
            // if we get a 400, throw an Api exception
            JsonParser parser = m_mapper.getFactory().createParser(resp.body());
            if (!JsonToken.START_ARRAY.equals(parser.nextToken())) {
                logger.warn("Malformed response, using control values.");
                return;
            }
            parser.nextToken();
            for (Requestable request : requests) {
                if (parser.currentToken().equals(JsonToken.END_ARRAY)) {
                    logger.warn("Response too short, using control values.");
                }
                if (parser.currentToken().equals(JsonToken.VALUE_STRING)) {
                    if (parser.getText().equals("OFF")) {
                        request.setActive(false);
                        parser.nextToken();
                        continue;
                    } else if (parser.getText().equals("UNKNOWN")) {
                        // server doesn't know about this feature yet
                        logger.info("Server doesn't know feature " + request.featureName()
                                + ", using control.");
                        parser.nextToken();
                        continue;
                    }
                }
                if (!parser.currentToken().equals(JsonToken.START_OBJECT)) {
                    logger.warn("Malformed response for " + request.featureName()
                            + ", using control values.");
                    consumeValue(parser);
                }
                try {
                    request.deserializeResponse(parser);
                } catch (ApiException e) {
                    logger.warn("Error parsing response from server for " + request.featureName()
                            + ", reverting to control.");
                }
            }
        } catch (IOException e) {
            logger.warn("IO Exception processing server response, reverting to control.", e);
        }


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
    public void signal(JsonGenerator gen) throws InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(m_impressionServerUrl + "/signal"))
                .setHeader("user-agent", "Causal java client")
                .header("Content-Type", "application/json").header("Accept", "text/plain")
                .POST(BodyPublishers.ofString(getResult(gen))).build();
        m_client.sendAsync(req, BodyHandlers.ofString()).thenAccept(resp -> {
            if (resp.statusCode() != 200) {
                logger.error("Error signaling event: " + resp);
            }
        });
    }

    private String m_impressionServerUrl;
    HttpClient m_client = HttpClient.newHttpClient();
    public static final Logger logger = LoggerFactory.getLogger(CausalClientBase.class);
    public static final ObjectMapper m_mapper = new ObjectMapper();
}

