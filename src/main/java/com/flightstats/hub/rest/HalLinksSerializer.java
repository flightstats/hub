package com.flightstats.hub.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class HalLinksSerializer extends JsonSerializer<HalLinks> {

    @Override
    public void serialize(HalLinks halLinks, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        serializeLinks(halLinks, jgen);
        serializeMultiLinks(halLinks, jgen);
        jgen.writeEndObject();
    }

    private void serializeLinks(HalLinks halLinks, JsonGenerator jgen) throws IOException {
        for (HalLink link : halLinks.getLinks()) {
            jgen.writeFieldName(link.getName());
            jgen.writeStartObject();
            jgen.writeStringField("href", link.getUri().toString());
            jgen.writeEndObject();
        }
    }

    private void serializeMultiLinks(HalLinks halLinks, JsonGenerator jgen) throws IOException {
        Multimap<String, HalLink> multiLinks = halLinks.getMultiLinks();
        for (Map.Entry<String, Collection<HalLink>> entry : multiLinks.asMap().entrySet()) {
            String linkName = entry.getKey();
            Collection<HalLink> links = entry.getValue();
            jgen.writeFieldName(linkName);
            jgen.writeStartArray();
            for (HalLink link : links) {
                jgen.writeStartObject();
                jgen.writeStringField("name", link.getName());
                jgen.writeStringField("href", link.getUri().toString());
                jgen.writeEndObject();
            }
            jgen.writeEndArray();
        }
    }

}
