package com.flightstats.datahub.service.exceptions;

import com.flightstats.datahub.model.exception.NoSuchChannelException;
import com.flightstats.services.common.jersey.exception.mappers.AbstractExceptionMapper;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class NoSuchChannelExceptionMapper extends AbstractExceptionMapper<NoSuchChannelException> {

	protected Response.Status getResponseCode() {
		return Response.Status.NOT_FOUND;
	}
}
