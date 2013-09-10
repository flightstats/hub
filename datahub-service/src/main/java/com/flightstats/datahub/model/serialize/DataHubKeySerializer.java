package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;

import java.io.IOException;

public class DataHubKeySerializer extends JsonSerializer<DataHubKey> {

    private final DataHubKeyRenderer dataHubKeyRenderer;

    public DataHubKeySerializer(DataHubKeyRenderer dataHubKeyRenderer) {
        this.dataHubKeyRenderer = dataHubKeyRenderer;
    }

    @Override
    public void serialize(DataHubKey value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        String keyString = dataHubKeyRenderer.keyToString(value);
        jgen.writeString(keyString);

    }
}
