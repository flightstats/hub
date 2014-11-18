package com.flightstats.hub.service.exceptions;

import com.flightstats.hub.util.AbstractExceptionMapper;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.EOFException;

/**
 * This exception comes up when there is no body at all in a PUT/POST request. Since such requests never even make it to
 * our handlers, the decision is to map all of these to a 400 rather than let it ripple back as a 500.
 *
 * A side effect of this is that if an EOFException escapes from our own code for some reason, it'll cause a return of 400
 * instead of a 500. That would be most unfortunate. Don't let your EOFExceptions escape.
 */
@Provider
@Singleton
public class EOFExceptionMapper extends AbstractExceptionMapper<EOFException> {

	protected Response.Status getResponseCode() { return Response.Status.BAD_REQUEST; }
}
