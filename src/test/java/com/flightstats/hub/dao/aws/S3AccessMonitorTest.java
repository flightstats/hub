package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

public class S3AccessMonitorTest {
    private final S3BucketName s3BucketName = new S3BucketName("test", "bucket");
    private HubS3Client s3Client;
    private S3AccessMonitor monitor;
    private PutObjectResult putObjectResult;
    private S3Object s3Object;

    @Before
    public void setUpTest() {
        s3Client = mock(HubS3Client.class);
        Dao<ChannelConfig> channelConfigDao = mock(DynamoChannelConfigDao.class);
        monitor = new S3AccessMonitor(channelConfigDao, s3Client, s3BucketName);
        s3Object = new S3Object();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("versionId", "testVersionId");
        s3Object.setObjectMetadata(metadata);
        putObjectResult = new PutObjectResult();
        putObjectResult.setVersionId("testVersionId");
    }

    @Test
    public void testVerifyReadWriteAccess_errorPutObject_false() {
        doThrow(new RuntimeException("public void testVerifyReadWriteAccess_errorPutObject_false"))
                .when(s3Client).putObject(any(PutObjectRequest.class));
        assertFalse(monitor.verifyReadWriteAccess());
    }

    @Test
    public void testVerifyReadWriteAccess_errorGetObject_false() {
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        doThrow(new RuntimeException("testVerifyReadWriteAccess_errorGetObject_false"))
                .when(s3Client).getObject(any(GetObjectRequest.class));

        assertFalse(monitor.verifyReadWriteAccess());
    }


    @Test
    public void testVerifyReadWriteAccess_mockVersionId_true() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        assertTrue(monitor.verifyReadWriteAccess());
    }
}
