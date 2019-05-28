package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3AccessMonitorTest {
    @Mock
    private HubS3Client s3Client;
    @Mock
    private Dao<ChannelConfig> channelConfigDao;
    @Mock
    private S3Properties s3Properties;
    private S3AccessMonitor monitor;
    private PutObjectResult putObjectResult;
    private S3Object s3Object;

    @BeforeEach
    void setUpTest() {
        monitor = new S3AccessMonitor(channelConfigDao, s3Client, s3Properties);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("versionId", "testVersionId");

        s3Object = new S3Object();
        s3Object.setObjectMetadata(metadata);

        putObjectResult = new PutObjectResult();
        putObjectResult.setVersionId("testVersionId");
    }

    @Test
    void testVerifyReadWriteAccess_errorPutObject_false() {
        doThrow(new RuntimeException("testVerifyReadWriteAccess_errorPutObject_false"))
                .when(s3Client).putObject(any(PutObjectRequest.class));
        assertFalse(monitor.verifyReadWriteAccess());
    }

    @Test
    void testVerifyReadWriteAccess_errorGetObject_false() {
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        doThrow(new RuntimeException("testVerifyReadWriteAccess_errorGetObject_false"))
                .when(s3Client).getObject(any(GetObjectRequest.class));

        assertFalse(monitor.verifyReadWriteAccess());
    }


    @Test
    void testVerifyReadWriteAccess_mockVersionId_true() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        assertTrue(monitor.verifyReadWriteAccess());
    }

    @Test
    void testVerifyReadWriteAccess_closeCalledGreenField_true() throws IOException {
        S3Object mockS3Object = mock(S3Object.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        monitor.verifyReadWriteAccess();
        verify(mockS3Object).close();
    }

    @Test
    void testVerifyReadWriteAccess_closeCalledWithErrorOnGet_true() throws IOException {
        S3Object mockS3Object = mock(S3Object.class);
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mockS3Object)
                .thenThrow(new RuntimeException("testVerifyReadWriteAccess_closeCalledErrorOnGet_true"));
        assertFalse(monitor.verifyReadWriteAccess());
        verify(mockS3Object).close();
    }

    @Test
    void testVerifyReadWriteAccess_handlesNullSafely_false() throws IOException {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(null);
        assertFalse(monitor.verifyReadWriteAccess());
    }
}
