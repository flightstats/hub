package com.flightstats.datahub.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.model.serialize.*;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.jackson.ObjectMapperBuilder;
import com.flightstats.rest.*;
import com.google.inject.Provides;

import java.util.Date;

public class DataHubObjectMapperFactory {

    @Provides
    public static ObjectMapper construct() {

        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();
        ObjectMapper mapper = new ObjectMapperBuilder("datahub-service")
                .withVersion(new Version(1, 0, 0, null, "data hub", "service"))
                .withMixInPackage("com.flightstats.datahub")
                .withMixInPackage("com.flightstats.rest")
                .withSerializer(HalLinks.class, new HalLinksSerializer())
                .withSerializer(Date.class, new Rfc3339DateSerializer())
                .withSerializer(DataHubKey.class, new DataHubKeySerializer(dataHubKeyRenderer))
                .withDeserializer(DataHubKey.class, new DataHubKeyDeserializer(dataHubKeyRenderer))
                .build();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }
}
