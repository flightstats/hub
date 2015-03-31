package com.flightstats.hub.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractExceptionMapper.class);

    public AbstractExceptionMapper() {
    }

    public Response toResponse(T exception) {
        logger.info(exception.getMessage());
        Response.ResponseBuilder builder = Response.status(this.getResponseCode());
        builder.entity("{\"message\":\"channel can not be modified while replicating\"}");
        return builder.build();
    }

    protected abstract Response.Status getResponseCode();
}