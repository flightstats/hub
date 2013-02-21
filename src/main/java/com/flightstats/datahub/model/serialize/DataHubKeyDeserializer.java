package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;

public class DataHubKeyDeserializer extends JsonDeserializer<DataHubKey> {

    private final DataHubKeyRenderer dataHubKeyRenderer;

    public DataHubKeyDeserializer(DataHubKeyRenderer dataHubKeyRenderer) {
        this.dataHubKeyRenderer = dataHubKeyRenderer;
    }

    @Override
    public DataHubKey deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return null;
    }
}
