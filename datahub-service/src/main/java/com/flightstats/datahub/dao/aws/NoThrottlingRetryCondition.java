package com.flightstats.datahub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.retry.RetryUtils;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
*
*/
public class NoThrottlingRetryCondition implements RetryPolicy.RetryCondition {

    private final static Logger logger = LoggerFactory.getLogger(NoThrottlingRetryCondition.class);

    @Override
    public boolean shouldRetry(AmazonWebServiceRequest originalRequest,
                               AmazonClientException exception,
                               int retriesAttempted) {
        // Always retry on client exceptions caused by IOException
        if (exception.getCause() instanceof IOException) return true;

        // Only retry on a subset of service exceptions
        if (exception instanceof AmazonServiceException) {
            AmazonServiceException ase = (AmazonServiceException)exception;

            if (ase.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR
                    || ase.getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                return true;
            }

            if (RetryUtils.isThrottlingException(ase)) {
                if (PutItemRequest.class.isAssignableFrom(originalRequest.getClass())) {
                    PutItemRequest putItemRequest = (PutItemRequest) originalRequest;
                    logger.info("throttling put in table  " + putItemRequest.getTableName());
                } else {
                    logger.info("throttling exception " + originalRequest);
                }
                return false;
            }

            /*
             * Clock skew exception. If it is then we will get the time offset
             * between the device time and the server time to set the clock skew
             * and then retry the request.
             */
            if (RetryUtils.isClockSkewError(ase)) return true;
        }

        return false;
    }
}
