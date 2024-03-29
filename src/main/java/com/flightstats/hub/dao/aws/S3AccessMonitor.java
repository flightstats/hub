package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class S3AccessMonitor {

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

    private final Dao<ChannelConfig> channelConfigDao;
    private final HubS3Client hubS3Client;
    private final String bucketName;
    private final Content content;

    @Inject
    public S3AccessMonitor(
            @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
            @Named("MAIN") HubS3Client hubS3Client,
            S3Properties s3Properties) {
        this.channelConfigDao = channelConfigDao;
        this.hubS3Client = hubS3Client;
        this.bucketName = s3Properties.getBucketName();

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
        if (!channelConfigDao.exists(CHANNEL_NAME)) {
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
            PutObjectRequest request = new PutObjectRequest(bucketName, key(), content.getStream(), metadata);
            return CompletableFuture.supplyAsync(() -> hubS3Client.putObject(request));
        } catch (Exception e) {
            log.error("error writing to s3: ", e);
            throw e;
        }
    }

    private CompletableFuture<String> waitForRead() {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try (S3Object s3Object = hubS3Client
                        .getObject(new GetObjectRequest(bucketName, key()))) {
                    s3Object.getObjectContent().read(new byte[s3Object.getObjectContent().available()]);
                    return s3Object.getObjectMetadata().getVersionId();
                } catch (IOException e) {
                    log.warn("error closing connection to s3", e);
                    return StringUtils.EMPTY;
                }
            });
        } catch (Exception e) {
            log.error("error getting object from s3", e);
            throw e;
        }
    }

    public boolean verifyReadWriteAccess() {
        try {
            createChannelIfNotExist();
            waitForWrite()
                    .thenCompose(result -> waitForRead()).get();
        } catch (Exception e) {
            log.error("error reaching S3: ", e);
            return false;
        }
        return true;
    }
}
