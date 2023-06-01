package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
@Slf4j
public class AwsExceptionMapper implements ExceptionMapper<AmazonClientException> {
    @Override
    public Response toResponse(AmazonClientException exception) {
        log.warn("unhandled aws exception", exception);
        if (AwsErrors.isAwsError(exception)) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (AwsErrors.isAwsThrottling(exception)) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .header("Retry-After", 60).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
