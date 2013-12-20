package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.flightstats.datahub.model.DataHubKey;

import java.io.IOException;

//todo - gfm - 12/20/13 - do we still need this?
public class DataHubKeySerializer extends JsonSerializer<DataHubKey> {

    public DataHubKeySerializer() {
    }

    @Override
    public void serialize(DataHubKey value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(value.keyToString());

    }
}
