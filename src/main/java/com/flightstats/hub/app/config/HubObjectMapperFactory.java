package com.flightstats.hub.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flightstats.rest.HalLinks;
import com.flightstats.rest.HalLinksSerializer;
import com.flightstats.rest.Rfc3339DateSerializer;
import com.google.inject.Provides;

import java.util.Date;

public class HubObjectMapperFactory {

    @Provides
    public static ObjectMapper construct() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(HalLinks.class, new HalLinksSerializer());
        module.addSerializer(Date.class, new Rfc3339DateSerializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }
}
