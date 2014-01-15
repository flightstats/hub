package com.flightstats.datahub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.google.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class AwsExceptionMapper implements ExceptionMapper<AmazonClientException> {

    @Override
    public Response toResponse(AmazonClientException exception) {
        if (AwsUtils.isAwsError(exception))  {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (AwsUtils.isAwsThrottling(exception)) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .header("Retry-After", 60).build();
        }
        //todo - gfm - 1/15/14 - is this what we want to default to?
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
