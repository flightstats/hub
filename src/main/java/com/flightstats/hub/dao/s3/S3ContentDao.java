package com.flightstats.hub.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.github.rholder.retry.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class S3ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3ContentDao.class);

    private final AmazonS3 s3Client;
    private final boolean useEncrypted;
    private final String s3BucketName;
    private final Retryer<Content> contentRetryer;

    @Inject
    public S3ContentDao(AmazonS3 s3Client, @Named("app.encrypted") boolean useEncrypted,
                        S3BucketName s3BucketName,
                        @Named("s3.content_backoff_wait") int content_backoff_wait,
                        @Named("s3.content_backoff_times") int content_backoff_times) {
        this.s3Client = s3Client;
        this.useEncrypted = useEncrypted;
        this.s3BucketName = s3BucketName.getS3BucketName();
        contentRetryer = buildRetryer(content_backoff_wait, content_backoff_times);
    }

    void initialize() {
        logger.info("checking if bucket exists " + s3BucketName);
        if (s3Client.doesBucketExist(s3BucketName)) {
            logger.info("bucket exists " + s3BucketName);
            return;
        }
        logger.error("EXITING! unable to find bucket " + s3BucketName);
        throw new RuntimeException("unable to find bucket " + s3BucketName);
    }


    void writeS3(String channelName, Content content, ContentKey key) {
        String s3Key = getS3ContentKey(channelName, key);
        InputStream stream = new ByteArrayInputStream(content.getData());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.getData().length);
        if (content.getContentType().isPresent()) {
            metadata.setContentType(content.getContentType().get());
            metadata.addUserMetadata("type", content.getContentType().get());
        } else {
            metadata.addUserMetadata("type", "none");
        }
        if (content.getContentLanguage().isPresent()) {
            metadata.addUserMetadata("language", content.getContentLanguage().get());
        } else {
            metadata.addUserMetadata("language", "none");
        }
        if (content.getUser().isPresent()) {
            metadata.addUserMetadata("user", content.getUser().get());
        }
        metadata.addUserMetadata("millis", String.valueOf(content.getMillis()));
        if (useEncrypted) {
            metadata.setServerSideEncryption(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        }
        PutObjectRequest request = new PutObjectRequest(s3BucketName, s3Key, stream, metadata);
        s3Client.putObject(request);
    }

    Content read(final String channelName, final ContentKey key) {
        try {
            return contentRetryer.call(new Callable<Content>() {
                @Override
                public Content call() throws Exception {
                    S3Object object = s3Client.getObject(s3BucketName, getS3ContentKey(channelName, key));
                    byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
                    ObjectMetadata metadata = object.getObjectMetadata();
                    Map<String, String> userData = metadata.getUserMetadata();
                    Content.Builder builder = Content.builder();
                    String type = userData.get("type");
                    if (!type.equals("none")) {
                        builder.withContentType(type);
                    }
                    String language = userData.get("language");
                    if (!language.equals("none")) {
                        builder.withContentLanguage(language);
                    }
                    if (userData.containsKey("user")) {
                        builder.withUser(userData.get("user"));
                    }
                    Long millis = Long.valueOf(userData.get("millis"));
                    builder.withData(bytes).withMillis(millis);
                    return builder.build();
                }
            });
        } catch (ExecutionException e) {
            if (AmazonClientException.class.isAssignableFrom(e.getCause().getClass())) {
                logger.info("unable to get " + channelName + " " + key.keyToString() + " " + e.getMessage());
            } else {
                logger.warn("unable to get " + channelName + " " + key.keyToString(), e);
            }
        } catch (RetryException e) {
            logger.warn("retried max times for " + channelName + " " + key.keyToString());
        }
        return null;
    }

    private String getS3ContentKey(String channelName, ContentKey key) {
        return channelName + "/content/" + key.keyToString();
    }

    @VisibleForTesting
    static Retryer<Content> buildRetryer(int multiplier, int attempts) {
        return RetryerBuilder.<Content>newBuilder()
                .retryIfException(new Predicate<Throwable>() {
                    @Override
                    public boolean apply(@Nullable Throwable input) {
                        logger.debug("exception! " + input);
                        if (input == null) return false;
                        if (AmazonS3Exception.class.isAssignableFrom(input.getClass())) {
                            int statusCode = ((AmazonS3Exception) input).getStatusCode();
                            return statusCode == 404 || statusCode == 403;
                        }
                        return false;
                    }
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(multiplier, 1, TimeUnit.MINUTES))
                .withStopStrategy(StopStrategies.stopAfterAttempt(attempts))
                .build();
    }

    public void delete(String channel) {
        final String channelPath = channel + "/";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (internalDelete(channelPath)) {

                    }
                    internalDelete(channelPath);
                    logger.info("completed deletion of " + channelPath );
                } catch (Exception e) {
                    logger.warn("unable to delete " + channelPath + " in " + s3BucketName, e);
                }
            }
        }).start();
    }

    private boolean internalDelete(String channelPath) {
        ListObjectsRequest request = new ListObjectsRequest();
        request.withBucketName(s3BucketName);
        request.withPrefix(channelPath);
        ObjectListing listing = s3Client.listObjects(request);
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (S3ObjectSummary objectSummary : listing.getObjectSummaries()) {
            keys.add(new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()));
        }
        if (keys.isEmpty()) {
            return false;
        }
        DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(s3BucketName);
        multiObjectDeleteRequest.setKeys(keys);
        try {
            s3Client.deleteObjects(multiObjectDeleteRequest);
            logger.info("deleting more from " + channelPath + " deleted " + keys.size());
        } catch (MultiObjectDeleteException e) {
            logger.info("what happened? " + channelPath, e);
            return true;
        }
        return listing.isTruncated();
    }
}
