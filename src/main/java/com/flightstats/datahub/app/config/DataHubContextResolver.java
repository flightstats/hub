package com.flightstats.datahub.app.config;

import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class DataHubContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper;

    public DataHubContextResolver() {
        objectMapper = new DataHubObjectMapperFactory().build();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
