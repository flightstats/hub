package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class S3AccessMonitor {
    private final Dao<ChannelConfig> channelConfigDao;
    private final HubS3Client hubS3Client;
    private final S3BucketName s3BucketName;
    private final Content content;
    private static final String CHANNEL_NAME = "S3_HEALTH_MONITOR";
    private static final byte[] SINGLE_BYTE_DATA = new byte[1];
    private static final ChannelConfig channelConfig = ChannelConfig
            .builder()
            .name(CHANNEL_NAME)
            .ttlDays(1)
            .protect(true)
            .description("internal use for monitoring s3 access health")
            .owner("INTERNAL")
            .build();

    @Inject
    public S3AccessMonitor(
            @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
            HubS3Client hubS3Client,
            S3BucketName s3BucketName) {
        this.channelConfigDao = channelConfigDao;
        this.hubS3Client = hubS3Client;
        this.s3BucketName = s3BucketName;

        DateTime insertTime = new DateTime();
        this.content = Content.builder()
                .withContentKey(new ContentKey(insertTime))
                .withData(SINGLE_BYTE_DATA)
                .withContentType("text/plain")
                .build();
    }

    private void createChannelIfNotExist() {
        /*
            use an actual channel to utilize built-in TTL strategy
            and to avoid naming collisions
        */
        if (!this.channelConfigDao.exists(CHANNEL_NAME)) {
            channelConfigDao.upsert(channelConfig);
        }
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

    public boolean verifyReadWriteAccess() {
        try {
            createChannelIfNotExist();
            waitForWrite()
                    .thenCompose(putObjectResult -> waitForRead(putObjectResult.getVersionId())).get();
        } catch (Exception e) {
            log.error("error reaching S3: ", e);
            return false;
        }
        return true;
    }
}
