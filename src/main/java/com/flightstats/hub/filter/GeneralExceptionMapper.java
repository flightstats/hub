package com.flightstats.hub.filter;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * Traps all errors including service level erros for reporting and trapping.
 */
@SuppressWarnings("WeakerAccess")
@Provider
@Slf4j
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        log.warn("exception", exception);
        Response response;
        if (exception instanceof WebApplicationException) {
            WebApplicationException webEx = (WebApplicationException) exception;
            response = webEx.getResponse();
        } else {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("500 Internal error").type("text/plain").build();
        }

        return response;

    }
}
