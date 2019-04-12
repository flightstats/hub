package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

public class S3AccessMonitorTest {
    private final S3BucketName s3BucketName = new S3BucketName("test", "bucket");

    @Test
    public void testVerifyReadWriteAccess_errorPutObject_false() {
        HubS3Client s3Client = mock(HubS3Client.class);
        S3AccessMonitor monitor = new S3AccessMonitor(s3Client, s3BucketName);
        doThrow(new RuntimeException("public void testVerifyReadWriteAccess_errorPutObject_false"))
                .when(s3Client).putObject(any(PutObjectRequest.class));

        assertFalse(monitor.verifyReadWriteAccess());
    }

    @Test
    public void testVerifyReadWriteAccess_errorGetObject_false() {
        HubS3Client s3Client = mock(HubS3Client.class);
        S3AccessMonitor monitor = new S3AccessMonitor(s3Client, s3BucketName);
        PutObjectResult putObjectResult = new PutObjectResult();
        putObjectResult.setVersionId("testVersionId");
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        doThrow(new RuntimeException("testVerifyReadWriteAccess_errorGetObject_false"))
                .when(s3Client).getObject(any(GetObjectRequest.class));

        assertFalse(monitor.verifyReadWriteAccess());
    }


    @Test
    public void testVerifyReadWriteAccess_mockVersionId_true() {
        HubS3Client s3Client = mock(HubS3Client.class);
        S3AccessMonitor monitor = new S3AccessMonitor(s3Client, s3BucketName);
        PutObjectResult putObjectResult = new PutObjectResult();
        putObjectResult.setVersionId("testVersionId");
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        assertTrue(monitor.verifyReadWriteAccess());
    }

    @Test
    public void testVerifyReadWriteAccess_SwallowDeleteObjectError_true() {
        HubS3Client s3Client = mock(HubS3Client.class);
        S3AccessMonitor monitor = new S3AccessMonitor(s3Client, s3BucketName);
        PutObjectResult putObjectResult = new PutObjectResult();
        putObjectResult.setVersionId("testVersionId");
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        doThrow(new RuntimeException("should get swallowed")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));
        assertTrue(monitor.verifyReadWriteAccess());
    }

    @Test
    public void testVerifyReadWriteAccess_SwallowDeleteVersionError_true() {
        HubS3Client s3Client = mock(HubS3Client.class);
        S3AccessMonitor monitor = new S3AccessMonitor(s3Client, s3BucketName);
        PutObjectResult putObjectResult = new PutObjectResult();
        putObjectResult.setVersionId("testVersionId");
        when(s3Client.putObject(any(PutObjectRequest.class))).thenReturn(putObjectResult);
        doThrow(new RuntimeException("should get swallowed")).when(s3Client).deleteVersion(any(DeleteVersionRequest.class));
        assertTrue(monitor.verifyReadWriteAccess());
    }
}
