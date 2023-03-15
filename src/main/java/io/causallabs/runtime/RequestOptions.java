package io.causallabs.runtime;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

/** Options that change the behavior of requests to an impression server. */
public class RequestOptions {

  public static Builder builder() {
    return new Builder();
  }
  ;

  public static class Builder {

    /**
     * Should the impression server stifle an error if an impression with the given impression ID is
     * not found. Should be set when the impression id we are signalling on may not exist in Causal
     * to stifle log errors
     *
     * @param x
     * @return
     */
    public Builder ignoreMissingImpression(boolean x) {
      m_obj.m_ignoreMissingImp = x;
      return this;
    }

    public RequestOptions build() {
      return m_obj;
    }

    private Builder() {
      m_obj = new RequestOptions();
    }

    RequestOptions m_obj = new RequestOptions();
  }

  public void serialize(JsonGenerator gen) throws IOException {
    gen.writeFieldName("options");
    gen.writeStartObject();
    gen.writeObjectField("ignore_missing_imp", m_ignoreMissingImp);
    gen.writeEndObject();
  }

  public boolean m_ignoreMissingImp = false;

  private RequestOptions() {}

  /**
   * Deserialize the options on the server side
   *
   * @param jsonNode
   */
  public RequestOptions(JsonNode jsonNode) {
    if (jsonNode.has("ignore_missing_imp")) {
      m_ignoreMissingImp = jsonNode.get("ignore_missing_imp").asBoolean();
    }
  }

  public static RequestOptions DEFAULTS = new RequestOptions();
}
