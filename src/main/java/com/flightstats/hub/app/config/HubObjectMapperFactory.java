package com.flightstats.hub.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.flightstats.jackson.ObjectMapperBuilder;
import com.flightstats.rest.HalLinks;
import com.flightstats.rest.HalLinksSerializer;
import com.flightstats.rest.Rfc3339DateSerializer;
import com.google.inject.Provides;

import java.util.Date;

public class HubObjectMapperFactory {

    @Provides
    public static ObjectMapper construct() {

        ObjectMapper mapper = new ObjectMapperBuilder("hub-service")
                .withSerializer(HalLinks.class, new HalLinksSerializer())
                .withSerializer(Date.class, new Rfc3339DateSerializer())
                .build();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }
}
