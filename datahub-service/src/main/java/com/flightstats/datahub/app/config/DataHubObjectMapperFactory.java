package com.flightstats.datahub.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.flightstats.jackson.ObjectMapperBuilder;
import com.flightstats.rest.HalLinks;
import com.flightstats.rest.HalLinksSerializer;
import com.flightstats.rest.Rfc3339DateSerializer;
import com.google.inject.Provides;

import java.util.Date;

public class DataHubObjectMapperFactory {

    @Provides
    public static ObjectMapper construct() {

        ObjectMapper mapper = new ObjectMapperBuilder("datahub-service")
                .withVersion(new Version(1, 0, 0, null, "data hub", "service"))
                .withMixInPackage("com.flightstats.datahub")
                .withMixInPackage("com.flightstats.rest")
                .withSerializer(HalLinks.class, new HalLinksSerializer())
                .withSerializer(Date.class, new Rfc3339DateSerializer())
                .build();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }
}
