package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;

import javax.ws.rs.ext.ContextResolver;

@javax.ws.rs.ext.Provider
@Singleton
class ObjectMapperResolver implements ContextResolver<ObjectMapper> {
    private final ObjectMapper objectMapper;

    public ObjectMapperResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
