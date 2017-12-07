package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.NoOpMetricsService;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HubS3ClientTest {

    @Test
    public void countError() throws Exception {
        S3BucketName s3BucketName = mock(S3BucketName.class);
        S3ResponseMetadata metadata = mock(S3ResponseMetadata.class);
        String requestId = "numbers-and-letters-go-here";
        when(metadata.getRequestId()).thenReturn(requestId);
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        AmazonWebServiceRequest request = mock(AmazonWebServiceRequest.class);
        when(amazonS3Client.getCachedResponseMetadata(request)).thenReturn(metadata);
        MetricsService metricsService = spy(new NoOpMetricsService());
        HubS3Client hubS3Client = new HubS3Client(s3BucketName, amazonS3Client, metricsService);
        SdkClientException exception = new AmazonS3Exception("something f'd up");

        hubS3Client.countError(exception, request, "fauxMethod", Collections.singletonList("foo:bar"));

        verify(metricsService).count(
                "s3.error",
                1,
                "exception:com.amazonaws.services.s3.model.AmazonS3Exception",
                "method:fauxMethod",
                "requestId:" + requestId,
                "foo:bar"
        );
    }
}