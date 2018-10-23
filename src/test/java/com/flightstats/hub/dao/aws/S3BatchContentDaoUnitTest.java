package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ContentKey;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class S3BatchContentDaoUnitTest {

    private static final String ORIGINAL_BUCKET = "bucketOne";
    private static final String ARCHIVE_BUCKET = "archive-bucketOne";
    private static final String CHANNEL_NAME = "test";
    private static final ContentKey BATCH_KEY = new ContentKey(1999, 12, 31, 23, 59);

    @Test
    public void testArchiveBatchSuccess() {
        StatsdReporter statsdReporter = mock(StatsdReporter.class);

        S3BucketName s3BucketName = mock(S3BucketName.class);
        when(s3BucketName.getS3BucketName()).thenReturn(ORIGINAL_BUCKET);

        String indexKey = S3BatchContentDao.getS3BatchIndexMinuteKey(CHANNEL_NAME, BATCH_KEY);
        String itemsKey = S3BatchContentDao.getS3BatchItemsMinuteKey(CHANNEL_NAME, BATCH_KEY);

        S3ObjectInputStream indexStream = mock(S3ObjectInputStream.class);
        S3ObjectInputStream itemsStream = mock(S3ObjectInputStream.class);

        ObjectMetadata indexMetadata = mock(ObjectMetadata.class);
        ObjectMetadata itemsMetadata = mock(ObjectMetadata.class);

        S3Object indexObject = mock(S3Object.class);
        when(indexObject.getObjectContent()).thenReturn(indexStream);
        when(indexObject.getObjectMetadata()).thenReturn(indexMetadata);

        S3Object itemsObject = mock(S3Object.class);
        when(itemsObject.getObjectContent()).thenReturn(itemsStream);
        when(itemsObject.getObjectMetadata()).thenReturn(itemsMetadata);

        HubS3Client hubS3Client = mock(HubS3Client.class);
        when(hubS3Client.getObject(ORIGINAL_BUCKET, indexKey)).thenReturn(indexObject);
        when(hubS3Client.getObject(ORIGINAL_BUCKET, itemsKey)).thenReturn(itemsObject);
        when(hubS3Client.putObject(ARCHIVE_BUCKET, indexKey, indexObject)).thenReturn(mock(PutObjectResult.class));
        when(hubS3Client.putObject(ARCHIVE_BUCKET, itemsKey, itemsObject)).thenReturn(mock(PutObjectResult.class));

        S3BatchContentDao dao = new S3BatchContentDao(hubS3Client, s3BucketName, statsdReporter);

        dao.archiveBatch(CHANNEL_NAME, BATCH_KEY);
    }

    @Test(expected = AmazonClientException.class)
    public void testArchiveBatchFailedGet() {
        StatsdReporter statsdReporter = mock(StatsdReporter.class);

        S3BucketName s3BucketName = mock(S3BucketName.class);
        when(s3BucketName.getS3BucketName()).thenReturn(ORIGINAL_BUCKET);

        String indexKey = S3BatchContentDao.getS3BatchIndexMinuteKey(CHANNEL_NAME, BATCH_KEY);

        HubS3Client hubS3Client = mock(HubS3Client.class);
        when(hubS3Client.getObject(ORIGINAL_BUCKET, indexKey)).thenThrow(new AmazonClientException("something bad happened"));

        S3BatchContentDao dao = new S3BatchContentDao(hubS3Client, s3BucketName, statsdReporter);

        dao.archiveBatch(CHANNEL_NAME, BATCH_KEY);

        verify(hubS3Client, never()).putObject(any());
        verify(hubS3Client, never()).deleteObject(any());
    }

    @Test(expected = AmazonClientException.class)
    public void testArchiveBatchFailedPut() {
        StatsdReporter statsdReporter = mock(StatsdReporter.class);

        S3BucketName s3BucketName = mock(S3BucketName.class);
        when(s3BucketName.getS3BucketName()).thenReturn(ORIGINAL_BUCKET);

        String indexKey = S3BatchContentDao.getS3BatchIndexMinuteKey(CHANNEL_NAME, BATCH_KEY);
        String itemsKey = S3BatchContentDao.getS3BatchItemsMinuteKey(CHANNEL_NAME, BATCH_KEY);

        S3ObjectInputStream indexStream = mock(S3ObjectInputStream.class);
        S3ObjectInputStream itemsStream = mock(S3ObjectInputStream.class);

        ObjectMetadata indexMetadata = mock(ObjectMetadata.class);
        ObjectMetadata itemsMetadata = mock(ObjectMetadata.class);

        S3Object indexObject = mock(S3Object.class);
        when(indexObject.getObjectContent()).thenReturn(indexStream);
        when(indexObject.getObjectMetadata()).thenReturn(indexMetadata);

        S3Object itemsObject = mock(S3Object.class);
        when(itemsObject.getObjectContent()).thenReturn(itemsStream);
        when(itemsObject.getObjectMetadata()).thenReturn(itemsMetadata);

        HubS3Client hubS3Client = mock(HubS3Client.class);
        when(hubS3Client.getObject(ORIGINAL_BUCKET, indexKey)).thenReturn(indexObject);
        when(hubS3Client.getObject(ORIGINAL_BUCKET, itemsKey)).thenReturn(itemsObject);
        when(hubS3Client.putObject(ARCHIVE_BUCKET, indexKey, indexObject)).thenThrow(new AmazonClientException("something bad happened"));

        S3BatchContentDao dao = new S3BatchContentDao(hubS3Client, s3BucketName, statsdReporter);

        dao.archiveBatch(CHANNEL_NAME, BATCH_KEY);

        verify(hubS3Client, never()).deleteObject(any());
    }

    @Test(expected = AmazonClientException.class)
    public void testArchiveBatchFailedDelete() {
        StatsdReporter statsdReporter = mock(StatsdReporter.class);

        S3BucketName s3BucketName = mock(S3BucketName.class);
        when(s3BucketName.getS3BucketName()).thenReturn(ORIGINAL_BUCKET);

        String indexKey = S3BatchContentDao.getS3BatchIndexMinuteKey(CHANNEL_NAME, BATCH_KEY);
        String itemsKey = S3BatchContentDao.getS3BatchItemsMinuteKey(CHANNEL_NAME, BATCH_KEY);

        S3ObjectInputStream indexStream = mock(S3ObjectInputStream.class);
        S3ObjectInputStream itemsStream = mock(S3ObjectInputStream.class);

        ObjectMetadata indexMetadata = mock(ObjectMetadata.class);
        ObjectMetadata itemsMetadata = mock(ObjectMetadata.class);

        S3Object indexObject = mock(S3Object.class);
        when(indexObject.getObjectContent()).thenReturn(indexStream);
        when(indexObject.getObjectMetadata()).thenReturn(indexMetadata);

        S3Object itemsObject = mock(S3Object.class);
        when(itemsObject.getObjectContent()).thenReturn(itemsStream);
        when(itemsObject.getObjectMetadata()).thenReturn(itemsMetadata);

        HubS3Client hubS3Client = mock(HubS3Client.class);
        when(hubS3Client.getObject(ORIGINAL_BUCKET, indexKey)).thenReturn(indexObject);
        when(hubS3Client.getObject(ORIGINAL_BUCKET, itemsKey)).thenReturn(itemsObject);
        when(hubS3Client.putObject(ARCHIVE_BUCKET, indexKey, indexObject)).thenReturn(mock(PutObjectResult.class));
        when(hubS3Client.putObject(ARCHIVE_BUCKET, itemsKey, itemsObject)).thenReturn(mock(PutObjectResult.class));
        doThrow(new AmazonClientException("something bad happened")).when(hubS3Client).deleteObject(ORIGINAL_BUCKET, indexKey);

        S3BatchContentDao dao = new S3BatchContentDao(hubS3Client, s3BucketName, statsdReporter);

        dao.archiveBatch(CHANNEL_NAME, BATCH_KEY);
    }
}
