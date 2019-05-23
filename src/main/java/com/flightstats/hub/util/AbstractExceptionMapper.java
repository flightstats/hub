package com.flightstats.hub.util;

import com.flightstats.hub.metrics.ActiveTraces;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@Slf4j
public abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    protected AbstractExceptionMapper() {
    }

    public Response toResponse(T exception) {
        ActiveTraces.getLocal().add(exception);
        ActiveTraces.getLocal().log(log);
        log.trace("exception", exception);
        ActiveTraces.end();
        Response.ResponseBuilder builder = Response.status(this.getResponseCode());
        builder.entity(exception.getMessage());
        return builder.build();
    }

    protected abstract Response.Status getResponseCode();
}