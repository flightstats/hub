package com.flightstats.datahub.service.exceptions;

import com.flightstats.datahub.model.exception.NoSuchChannelException;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class NoSuchChannelExceptionMapper extends AbstractExceptionMapper<NoSuchChannelException> {

	protected Response.Status getResponseCode() { return Response.Status.NOT_FOUND; }
}
