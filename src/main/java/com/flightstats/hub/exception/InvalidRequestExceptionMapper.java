package com.flightstats.hub.exception;

import com.flightstats.hub.util.AbstractExceptionMapper;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class InvalidRequestExceptionMapper extends AbstractExceptionMapper<InvalidRequestException> {

	protected Response.Status getResponseCode() { return Response.Status.BAD_REQUEST; }
}
