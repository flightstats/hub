package com.flightstats.datahub.service.exceptions;

import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class InvalidRequestExceptionMapper extends AbstractExceptionMapper<InvalidRequestException> {

	protected Response.Status getResponseCode() { return Response.Status.BAD_REQUEST; }
}
