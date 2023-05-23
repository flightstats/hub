package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.retry.RetryUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import java.io.IOException;

class AwsErrors {

    static boolean isAwsError(AmazonClientException exception) {
        if (exception.getCause() instanceof IOException) return true;

        if (exception instanceof AmazonServiceException) {
            AmazonServiceException ase = (AmazonServiceException) exception;

            if (ase.getStatusCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                return true;
            }

            //com.amazonaws.services.s3.model.AmazonS3Exception: Status Code: 400, AWS Service: Amazon S3,
            // AWS Error Code: RequestTimeout, AWS Error Message: Your socket connection to the server was not read from or written to within the timeout period. Idle connections will be closed.
            if (StringUtils.contains(ase.getErrorCode(), "RequestTimeout")) {
                return true;
            }

            /*
             * Clock skew exception. If it is then we will get the time offset
             * between the device time and the server time to set the clock skew
             * and then retry the request.
             */
            return RetryUtils.isClockSkewError(exception);
        }

        return false;
    }

    static boolean isAwsThrottling(AmazonClientException exception) {
        return RetryUtils.isThrottlingException(exception);
    }
}
