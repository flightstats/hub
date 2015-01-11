package com.flightstats.hub.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.Date;

public class Rfc3339DateSerializer extends JsonSerializer<Date> {

    private final DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withZoneUTC();

    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        DateTime datetime = new DateTime(value);
        String formattedTime = formatter.print(datetime);
        jgen.writeString(formattedTime);
    }
}
