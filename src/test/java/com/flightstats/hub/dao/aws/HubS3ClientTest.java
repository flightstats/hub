package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.metrics.StatsdReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubS3ClientTest {

    private static final String METRIC_NAME = "s3.error";
    private static final int METRIC_VALUE = 1;

    @Mock
    private S3Properties s3Properties;
    @Mock
    private S3ResponseMetadata s3ResponseMetadata;

    @Test
    void countError() {
        String requestId = "numbers-and-letters-go-here";
        when(s3ResponseMetadata.getRequestId()).thenReturn(requestId);
        when(s3Properties.getBucketName()).thenReturn("testBucket");

        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        AmazonWebServiceRequest request = mock(AmazonWebServiceRequest.class);
        when(amazonS3Client.getCachedResponseMetadata(request)).thenReturn(s3ResponseMetadata);
        StatsdReporter statsdReporter = mock(StatsdReporter.class);

        HubS3Client hubS3Client = new HubS3Client(s3Properties, amazonS3Client, statsdReporter);
        SdkClientException exception = new AmazonS3Exception("something f'd up");
        hubS3Client.countError(exception, request, "fauxMethod", Collections.singletonList("foo:bar"));

        verify(statsdReporter).count(
                "s3.error",
                1,
                "exception:com.amazonaws.services.s3.model.AmazonS3Exception",
                "method:fauxMethod",
                "bucket:testBucket"
        );
    }

    @Test
    void putObject() {
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        StatsdReporter statsdReporter = mock(StatsdReporter.class);
        when(s3Properties.getBucketName()).thenReturn("testBucket");

        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        ObjectMetadata metadata = new ObjectMetadata();
        PutObjectRequest request = new PutObjectRequest("testBucket", "testKey", emptyStream, metadata);

        when(amazonS3Client.putObject(request)).thenThrow(new SdkClientException("testException"));

        HubS3Client hubS3Client = new HubS3Client(s3Properties, amazonS3Client, statsdReporter);


        try {
            hubS3Client.putObject(request);
            fail("expected SdkClientException to be thrown");
        } catch (SdkClientException e) {
            // this should happen
        }

        String[] tags = {
            "exception:com.amazonaws.SdkClientException",
            "method:putObject",
            "bucket:testBucket"
        };

        verify(statsdReporter).count(METRIC_NAME, METRIC_VALUE, tags);
    }
}