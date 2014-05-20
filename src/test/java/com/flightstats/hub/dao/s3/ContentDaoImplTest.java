package com.flightstats.hub.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.flightstats.hub.model.Content;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ContentDaoImplTest {
    private final static Logger logger = LoggerFactory.getLogger(ContentDaoImplTest.class);

    @Test
    public void testRetryerNull() throws Exception {
        Callable<Content> callable = mock(Callable.class);
        Retryer<Content> retryer = S3ContentDao.buildRetryer(1, 2);
        retryer.call(callable);
        verify(callable, new Times(1)).call();

    }

    @Test
    public void testRetryer404() throws Exception {
        Callable<Content> callable = mock(Callable.class);
        AmazonS3Exception exception = mock(AmazonS3Exception.class);
        when(exception.getStatusCode()).thenReturn(404);
        when(callable.call()).thenThrow(exception);
        Retryer<Content> retryer = S3ContentDao.buildRetryer(1, 2);
        try {
            retryer.call(callable);
            fail("should throw an exception");
        } catch (RetryException e) {
            //expected
        }
        verify(callable, new Times(2)).call();

    }

    @Test
    public void testRetryerAmazonClientException() throws Exception {
        Callable<Content> callable = mock(Callable.class);
        AmazonClientException exception = mock(AmazonClientException.class);
        when(callable.call()).thenThrow(exception);
        Retryer<Content> retryer = S3ContentDao.buildRetryer(1, 2);
        try {
            retryer.call(callable);
            fail("should throw an exception");
        } catch (ExecutionException e) {
            logger.info("cause " + e.getCause());
            logger.info(e.getMessage(), e);
        }
        verify(callable, new Times(1)).call();

    }

/*
    only run this to verify timing
    @Test
    public void testMaxWait() throws Exception {
        Callable<Content> callable = mock(Callable.class);
        AmazonS3Exception exception = mock(AmazonS3Exception.class);
        when(exception.getStatusCode()).thenReturn(404);
        when(callable.call()).thenThrow(exception);
        Retryer<Content> retryer = S3ContentDao.buildRetryer(1000, 6);
        long start = System.currentTimeMillis();
        try {
            retryer.call(callable);
            fail("should throw an exception");
        } catch (RetryException e) {
            //expected
        }
        logger.info("total time millis " + (System.currentTimeMillis() - start));
        verify(callable, new Times(6)).call();
    }*/
}