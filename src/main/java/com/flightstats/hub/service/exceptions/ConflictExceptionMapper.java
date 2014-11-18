package com.flightstats.hub.service.exceptions;

import com.flightstats.hub.model.exception.ConflictException;
import com.flightstats.hub.util.AbstractExceptionMapper;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class ConflictExceptionMapper extends AbstractExceptionMapper<ConflictException> {

	protected Response.Status getResponseCode() { return Response.Status.CONFLICT; }
}
