package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
*/
public class HubRetryCondition implements RetryPolicy.RetryCondition {

    private final static Logger logger = LoggerFactory.getLogger(HubRetryCondition.class);

    @Override
    public boolean shouldRetry(AmazonWebServiceRequest originalRequest,
                               AmazonClientException exception,
                               int retriesAttempted) {

        if (AwsUtils.isAwsError(exception)) {
            return true;
        }
        if (AwsUtils.isAwsThrottling(exception)) {
            if (PutItemRequest.class.isAssignableFrom(originalRequest.getClass())) {
                PutItemRequest putItemRequest = (PutItemRequest) originalRequest;
                logger.info("throttling put in table  " + putItemRequest.getTableName());
            } else {
                logger.info("throttling exception " + originalRequest);
            }
        }

        return false;
    }
}
