package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class S3AccessMonitor {
    private final HubS3Client hubS3Client;
    private final S3BucketName s3BucketName;
    private final Content content;
    private static final String CHANNEL_NAME = "S3_HEALTH_MONITOR";

    @Inject
    public S3AccessMonitor(HubS3Client hubS3Client, S3BucketName s3BucketName) {
        this.hubS3Client = hubS3Client;
        this.s3BucketName = s3BucketName;
        String data = "1";
        DateTime insertTime = new DateTime();
        this.content = Content.builder()
                .withContentKey(new ContentKey(insertTime))
                .withData(data.getBytes())
                .withContentType("text/plain")
                .build();
    }

    private String key() {
        Optional<String> url = content.getContentKey().map(ContentKey::toUrl);
        String keyPath = url.orElse("");
        return CHANNEL_NAME + "/" + keyPath;
    }

    private CompletableFuture<PutObjectResult> waitForWrite() {
        try {
            ObjectMetadata metadata = S3SingleContentDao.createObjectMetadata(content, false);
            metadata.setContentLength(content.getData().length);
            PutObjectRequest request = new PutObjectRequest(s3BucketName.getS3BucketName(), key(), content.getStream(), metadata);
            return CompletableFuture.supplyAsync(() -> hubS3Client.putObject(request));
        } catch (Exception e) {
            log.error("error writing to s3: ", e);
            throw e;
        }
    }

    private CompletableFuture<String> waitForRead(String versionId) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                hubS3Client.getObject(new GetObjectRequest(s3BucketName.getS3BucketName(), key(), versionId));
                return versionId;
            });
        } catch(Exception e) {
            log.error("error getting object from s3", e);
            throw e;
        }
    }

    private void cleanup(String versionId) {
        try {
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(s3BucketName.getS3BucketName(), key());
            hubS3Client.deleteObject(deleteObjectRequest);
            if (versionId != null) {
                DeleteVersionRequest deleteVersionRequest = new DeleteVersionRequest(s3BucketName.getS3BucketName(), key(), versionId);
                hubS3Client.deleteVersion(deleteVersionRequest);
            }
        } catch (Exception e) {
            log.error("error cleaning up items after s3 access verification: ", e);
        }
    }

    public boolean verifyReadWriteAccess() {
        try {
            waitForWrite()
                    .thenCompose(putObjectResult -> waitForRead(putObjectResult.getVersionId()))
                    .thenAccept(this::cleanup).get();
        } catch (Exception e) {
            log.error("error reaching S3: ", e);
            return false;
        }
        return true;
    }
}
