package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.NoOpMetricsService;
import com.flightstats.hub.metrics.StatsDHandlers;
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
    public void countError() {
        S3BucketName s3BucketName = mock(S3BucketName.class);
        S3ResponseMetadata metadata = mock(S3ResponseMetadata.class);
        String requestId = "numbers-and-letters-go-here";
        when(metadata.getRequestId()).thenReturn(requestId);
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        AmazonWebServiceRequest request = mock(AmazonWebServiceRequest.class);
        when(amazonS3Client.getCachedResponseMetadata(request)).thenReturn(metadata);
        StatsDHandlers statsDHandlers = mock(StatsDHandlers.class);
        HubS3Client hubS3Client = new HubS3Client(s3BucketName, amazonS3Client, statsDHandlers);
        SdkClientException exception = new AmazonS3Exception("something f'd up");

        hubS3Client.countError(exception, request, "fauxMethod", Collections.singletonList("foo:bar"));

        verify(statsDHandlers).count(
                "s3.error",
                1,
                "exception:com.amazonaws.services.s3.model.AmazonS3Exception",
                "method:fauxMethod",
                "requestId:" + requestId,
                "foo:bar"
        );
    }

    @Test
    public void putObject() {
        S3BucketName s3BucketName = mock(S3BucketName.class);
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        StatsDHandlers statsDHandlers = mock(StatsDHandlers.class);
        HubS3Client hubS3Client = new HubS3Client(s3BucketName, amazonS3Client, statsDHandlers);

        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        ObjectMetadata metadata = new ObjectMetadata();
        PutObjectRequest request = new PutObjectRequest("testBucket", "testKey", emptyStream, metadata);

        when(amazonS3Client.putObject(request)).thenThrow(new SdkClientException("testException"));

        try {
            hubS3Client.putObject(request);
            fail("expected SdkClientException to be thrown");
        } catch (SdkClientException e) {
            // this should happen
        }

        String[] tags = {
            "exception:com.amazonaws.SdkClientException",
            "method:putObject",
            "bucket:testBucket",
            "key:testKey"
        };

        verify(statsDHandlers).count(METRIC_NAME, METRIC_VALUE, tags);
    }
}