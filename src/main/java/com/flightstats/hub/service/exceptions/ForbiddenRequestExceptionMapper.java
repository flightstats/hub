package com.flightstats.hub.service.exceptions;

import com.flightstats.hub.model.exception.ForbiddenRequestException;
import com.flightstats.services.common.jersey.exception.mappers.AbstractExceptionMapper;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class ForbiddenRequestExceptionMapper extends AbstractExceptionMapper<ForbiddenRequestException> {

    protected Response.Status getResponseCode() { return Response.Status.FORBIDDEN; }
}