package com.flightstats.hub.service.exceptions;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.flightstats.services.common.jersey.exception.mappers.AbstractExceptionMapper;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class JsonMappingExceptionMapper extends AbstractExceptionMapper<JsonMappingException> {

	protected Response.Status getResponseCode() { return Response.Status.BAD_REQUEST; }
}
