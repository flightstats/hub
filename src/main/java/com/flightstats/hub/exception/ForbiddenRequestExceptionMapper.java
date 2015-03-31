package com.flightstats.hub.exception;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class ForbiddenRequestExceptionMapper implements ExceptionMapper<ForbiddenRequestException> {
    private final static Logger logger = LoggerFactory.getLogger(ForbiddenRequestExceptionMapper.class);

    public Response toResponse(ForbiddenRequestException exception) {
        logger.info(exception.getMessage());
        Response.ResponseBuilder builder = Response.status(this.getResponseCode());
        builder.entity("{\"message\":\"channel can not be modified while replicating\"}");
        return builder.build();
    }


    protected Response.Status getResponseCode() {
        return Response.Status.FORBIDDEN;
    }
}