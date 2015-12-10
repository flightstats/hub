package com.flightstats.hub.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.flightstats.hub.util.HubUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Date;

public class Rfc3339DateSerializer extends JsonSerializer<Date> {

    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(HubUtils.FORMATTER.print(new DateTime(value)));
    }
}
