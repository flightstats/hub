package com.flightstats.hub.exception;

import com.flightstats.hub.util.AbstractExceptionMapper;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class JsonSyntaxExceptionMapper extends AbstractExceptionMapper<JsonSyntaxException> {

    protected Response.Status getResponseCode() {
        return Response.Status.BAD_REQUEST;
    }
}
