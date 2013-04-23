package com.flightstats.datahub.service.exceptions;

import com.flightstats.datahub.model.exception.NoSuchChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NoSuchChannelExceptionMapper implements ExceptionMapper<NoSuchChannelException> {

	private final static Logger logger = LoggerFactory.getLogger(NoSuchChannelExceptionMapper.class);

	@Override
	public Response toResponse(NoSuchChannelException exception) {
		logger.info(exception.getMessage());
		Response.ResponseBuilder builder = Response.status(Response.Status.NOT_FOUND);
		builder.entity(exception.getMessage());
		return builder.build();
	}
}
