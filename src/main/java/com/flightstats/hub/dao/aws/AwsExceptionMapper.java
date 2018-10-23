package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
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
        if (AwsUtils.isAwsError(exception)) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (AwsUtils.isAwsThrottling(exception)) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .header("Retry-After", 60).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
