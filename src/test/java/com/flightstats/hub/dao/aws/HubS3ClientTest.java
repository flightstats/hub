package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.NoOpMetricsService;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HubS3ClientTest {

    private static final String METRIC_NAME = "s3.error";
    private static final int METRIC_VALUE = 1;

    @Test
    public void countErrorGetsAmazonServiceException() {
        S3BucketName s3BucketName = mock(S3BucketName.class);
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        MetricsService metricsService = spy(new NoOpMetricsService());
        HubS3Client hubS3Client = new HubS3Client(s3BucketName, amazonS3Client, metricsService);
        AmazonClientException exception = new AmazonClientException("something f'd up");

        hubS3Client.countError(exception, "fauxMethod", Collections.singletonList("foo:bar"));

        verify(metricsService).count(
                "s3.error",
                1,
                "exception:com.amazonaws.AmazonClientException",
                "method:fauxMethod",
                "foo:bar"
        );
    }

    @Test
    public void countErrorGetsAmazonClientException() {
        S3BucketName s3BucketName = mock(S3BucketName.class);
        String requestId = "numbers-and-letters-go-here";
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        MetricsService metricsService = spy(new NoOpMetricsService());
        HubS3Client hubS3Client = new HubS3Client(s3BucketName, amazonS3Client, metricsService);
        AmazonServiceException exception = new AmazonServiceException("something f'd up");
        exception.setRequestId(requestId);

        hubS3Client.countError(exception, "fauxMethod", Collections.singletonList("foo:bar"));

        verify(metricsService).count(
                "s3.error",
                1,
                "exception:com.amazonaws.AmazonServiceException",
                "method:fauxMethod",
                "requestId:" + requestId,
                "foo:bar"
        );
    }

    @Test
    public void putObject() {
        S3BucketName s3BucketName = mock(S3BucketName.class);
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        MetricsService metricsService = mock(MetricsService.class);
        HubS3Client hubS3Client = new HubS3Client(s3BucketName, amazonS3Client, metricsService);

        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        ObjectMetadata metadata = new ObjectMetadata();
        PutObjectRequest request = new PutObjectRequest("testBucket", "testKey", emptyStream, metadata);

        when(amazonS3Client.putObject(request)).thenThrow(new AmazonClientException("testException"));

        try {
            hubS3Client.putObject(request);
            fail("expected AmazonClientException to be thrown");
        } catch (AmazonClientException e) {
            // this should happen
        }

        String[] tags = {
            "exception:com.amazonaws.AmazonClientException",
            "method:putObject",
            "bucket:testBucket",
            "key:testKey"
        };

        verify(metricsService).count(METRIC_NAME, METRIC_VALUE, tags);
    }
}