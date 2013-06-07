package com.flightstats.datahub.service.exceptions;

import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.services.common.jersey.exception.mappers.AbstractExceptionMapper;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class AlreadyExistsExceptionMapper extends AbstractExceptionMapper<AlreadyExistsException> {

	protected Response.Status getResponseCode() { return Response.Status.CONFLICT; }
}
