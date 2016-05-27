package com.flightstats.hub.util;

import com.flightstats.hub.metrics.ActiveTraces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractExceptionMapper.class);

    protected AbstractExceptionMapper() {
    }

    public Response toResponse(T exception) {
        ActiveTraces.getLocal().add(exception);
        ActiveTraces.getLocal().log(logger);
        logger.trace("exception", exception);
        ActiveTraces.end();
        Response.ResponseBuilder builder = Response.status(this.getResponseCode());
        builder.entity(exception.getMessage());
        return builder.build();
    }

    protected abstract Response.Status getResponseCode();
}