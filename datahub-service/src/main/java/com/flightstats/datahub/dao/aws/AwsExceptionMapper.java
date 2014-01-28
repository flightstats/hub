package com.flightstats.datahub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class AwsExceptionMapper implements ExceptionMapper<AmazonClientException> {
    private final static Logger logger = LoggerFactory.getLogger(AwsExceptionMapper.class);

    @Override
    public Response toResponse(AmazonClientException exception) {
        logger.warn("unhandled aws exception", exception);
        if (AwsUtils.isAwsError(exception))  {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (AwsUtils.isAwsThrottling(exception)) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .header("Retry-After", 60).build();
        }
        //todo - gfm - 1/27/14 - is this really what we want to return?
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
