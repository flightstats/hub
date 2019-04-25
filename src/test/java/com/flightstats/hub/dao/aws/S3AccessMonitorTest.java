package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.PropertyLoader;
import com.flightstats.hub.config.S3Property;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class S3AccessMonitorTest {
    private final S3BucketName s3BucketName = new S3BucketName(
            new AppProperty(PropertyLoader.getInstance()),
            new S3Property(PropertyLoader.getInstance()));
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
        doThrow(new RuntimeException("testVerifyReadWriteAccess_errorPutObject_false"))
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

    @Test
    public void testVerifyReadWriteAccess_closeCalledGreenField_true() throws IOException {
        S3Object mockS3Object = mock(S3Object.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        monitor.verifyReadWriteAccess();
        verify(mockS3Object).close();
    }

    @Test
    public void testVerifyReadWriteAccess_closeCalledWithErrorOnGet_true() throws IOException {
        S3Object mockS3Object = mock(S3Object.class);
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mockS3Object)
                .thenThrow(new RuntimeException("testVerifyReadWriteAccess_closeCalledErrorOnGet_true"));
        assertFalse(monitor.verifyReadWriteAccess());
        verify(mockS3Object).close();
    }

    @Test
    public void testVerifyReadWriteAccess_handlesNullSafely_false() throws IOException {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(null);
        assertFalse(monitor.verifyReadWriteAccess());
    }
}
