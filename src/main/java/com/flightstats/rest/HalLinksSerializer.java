package com.flightstats.rest;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.io.IOException;

public class HalLinksSerializer extends JsonSerializer<HalLinks> {

    @Override
    public void serialize(HalLinks halLinks, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        for (HalLink link : halLinks.getLinks()) {
            jgen.writeFieldName(link.getName());
            jgen.writeStartObject();
            jgen.writeFieldName("href");
            jgen.writeObject(link.getUri().toString());
            jgen.writeEndObject();
        }
        jgen.writeEndObject();
    }
}
