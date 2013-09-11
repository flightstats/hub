package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;

import java.io.IOException;

public class DataHubKeyDeserializer extends JsonDeserializer<DataHubKey> {

    private final DataHubKeyRenderer dataHubKeyRenderer;

    public DataHubKeyDeserializer(DataHubKeyRenderer dataHubKeyRenderer) {
        this.dataHubKeyRenderer = dataHubKeyRenderer;
    }

    @Override
    public DataHubKey deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String value = jp.getText();
        return dataHubKeyRenderer.fromString(value);
    }
}
