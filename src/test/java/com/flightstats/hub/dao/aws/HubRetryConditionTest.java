package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 *
 */
@SuppressWarnings("ThrowableInstanceNeverThrown")
public class HubRetryConditionTest {

    private HubRetryCondition retryCondition;
    private AmazonWebServiceRequest request;

    @Before
    public void setUp() throws Exception {
        retryCondition = new HubRetryCondition();
        request = mock(AmazonWebServiceRequest.class);
    }

    @Test
    public void testIOException() throws Exception {
        AmazonClientException exception = new AmazonClientException("blah", new IOException());
        assertTrue(retryCondition.shouldRetry(request, exception, 0));
    }

    @Test
    public void testInternalServiceError() throws Exception {
        AmazonServiceException exception = new AmazonServiceException("blah");
        exception.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertTrue(retryCondition.shouldRetry(request, exception, 0));
    }

    @Test
    public void testServiceUnavailable() throws Exception {
        AmazonServiceException exception = new AmazonServiceException("blah");
        exception.setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
        assertTrue(retryCondition.shouldRetry(request, exception, 0));
    }

    @Test
    public void testThrottling() throws Exception {
        AmazonServiceException exception = new AmazonServiceException("blah");
        exception.setErrorCode("Throttling");
        assertFalse(retryCondition.shouldRetry(request, exception, 0));
    }

    @Test
    public void testRequestTimeout() throws Exception {
        AmazonServiceException exception = new AmazonS3Exception("blah");
        exception.setErrorCode("RequestTimeout");
        assertTrue(retryCondition.shouldRetry(request, exception, 0));
    }

    @Test
    public void testOther() throws Exception {
        AmazonServiceException exception = new AmazonS3Exception("blah");
        assertFalse(retryCondition.shouldRetry(request, exception, 0));
    }


}
