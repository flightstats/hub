package com.flightstats.hub.exception;

import com.flightstats.hub.util.AbstractExceptionMapper;
import com.google.inject.Singleton;
import org.joda.time.IllegalFieldValueException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class JodaTimeExceptionMapper extends AbstractExceptionMapper<IllegalFieldValueException> {

    protected Response.Status getResponseCode() {
        return Response.Status.BAD_REQUEST;
    }
}
