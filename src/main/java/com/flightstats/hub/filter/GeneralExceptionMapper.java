package com.flightstats.hub.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * Traps all errors including service level erros for reporting and trapping.
 */
@SuppressWarnings("WeakerAccess")
@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {
    private final static Logger logger = LoggerFactory.getLogger(GeneralExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        logger.warn("exception", exception);
        Response response = null;
        if (exception instanceof WebApplicationException) {
            WebApplicationException webEx = (WebApplicationException) exception;
            response = webEx.getResponse();
        } else {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("500 Internal error").type("text/plain").build();
        }

        return response;

    }
}
