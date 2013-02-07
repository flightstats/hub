package com.flightstats.datahub.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * HTTP 409
 */
public class AlreadyExistsException extends WebApplicationException {

    public AlreadyExistsException() {
        super(Response.status(Response.Status.CONFLICT).build());
    }
}
