package com.flightstats.hub.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.timeIndex.TimeIndex;
import com.flightstats.hub.dao.timeIndex.TimeIndexDao;
import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.metrics.TimedCallback;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.flightstats.hub.util.ContentKeyGenerator;
import com.github.rholder.retry.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This uses S3 for Content and ZooKeeper for TimeIndex
 */
public class S3ContentDao implements ContentDao, TimeIndexDao {

    private final static Logger logger = LoggerFactory.getLogger(S3ContentDao.class);

    private final ContentKeyGenerator keyGenerator;
    private final AmazonS3 s3Client;
    private final CuratorFramework curator;
    private final String s3BucketName;
    private final MetricsTimer metricsTimer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Retryer<Content> contentRetryer;

    @Inject
    public S3ContentDao(ContentKeyGenerator keyGenerator, AmazonS3 s3Client,
                        @Named("s3.environment") String environment, @Named("app.name") String appName,
                        @Named("s3.content_backoff_wait") int content_backoff_wait, @Named("s3.content_backoff_times") int content_backoff_times,
                        CuratorFramework curator, MetricsTimer metricsTimer) {
        this.keyGenerator = keyGenerator;
        this.s3Client = s3Client;
        this.curator = curator;
        this.metricsTimer = metricsTimer;
        this.s3BucketName = appName + "-" + environment;
        /**
         * 1000 ms and 6 times should give behavior of calls after 2s, 4s, 8s, 16s and 32s
         * This weights us towards returning sooner if we do get a result.
         * The total max wait time is 62s
         */
        contentRetryer = buildRetryer(content_backoff_wait, content_backoff_times);
        HubServices.register(new S3ContentDaoInit());
    }

    private class S3ContentDaoInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            initialize();
        }

        @Override
        protected void shutDown() throws Exception { }

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
                            AmazonS3Exception s3Exception = (AmazonS3Exception) input;
                            return s3Exception.getStatusCode() == 404;
                        }
                        return false;
                    }
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(multiplier, 1, TimeUnit.MINUTES))
                .withStopStrategy(StopStrategies.stopAfterAttempt(attempts))
                .build();
    }

    @Override
    public InsertedContentKey write(String channelName, Content content, long ttlDays) {
        if (!content.getContentKey().isPresent()) {
            content.setContentKey(keyGenerator.newKey(channelName));
        }
        ContentKey key = content.getContentKey().get();
        DateTime dateTime = new DateTime(content.getMillis());
        writeS3(channelName, content, key);
        writeIndex(channelName, dateTime, key);
        return new InsertedContentKey(key, dateTime.toDate());
    }

    public void writeIndex(String channelName, DateTime dateTime, ContentKey key) {
        final String path = TimeIndex.getPath(channelName, dateTime, key);
        metricsTimer.time("timeIndex.write", new TimedCallback<Object>() {
            @Override
            public Object call() {
                try {
                    curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                } catch (KeeperException.NodeExistsException ignore) {
                    //this can happen with rolling restarts
                    logger.info("node exits " + path);
                } catch (Exception e) {
                    logger.warn("unable to create " + path, e);
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }

    private void writeS3(String channelName, Content content, ContentKey key) {
        String s3Key = getS3ContentKey(channelName, key);
        //todo - gfm - 1/9/14 - this could use streaming if the content length is specified
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
        metadata.addUserMetadata("millis", String.valueOf(content.getMillis()));
        PutObjectRequest request = new PutObjectRequest(s3BucketName, s3Key, stream, metadata);
        s3Client.putObject(request);
    }

    @Override
    public Content read(final String channelName, final ContentKey key) {
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

    @Override
    public void writeIndices(String channelName, String dateTime, List<String> keys) {
        try {
            String s3Key = getS3IndexKey(channelName, dateTime);
            byte[] bytes = mapper.writeValueAsBytes(keys);
            InputStream stream = new ByteArrayInputStream(bytes);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(MediaType.APPLICATION_JSON);
            PutObjectRequest request = new PutObjectRequest(s3BucketName, s3Key, stream, metadata);
            s3Client.putObject(request);
        } catch (Exception e) {
            logger.warn("unable to create index " + channelName + dateTime + keys, e);
        }
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        /**
         * The TimeIndex is written to ZK, then TimeIndexProcessor write the data to S3, then deletes the keys.
         * We try reading from S3 first, because the ZK cache may be partial if it is already written to S3.
         * There is a bit of a race condition here, especially with S3 eventual consistency.
         */
        String hashTime = TimeIndex.getHash(dateTime);
        try {
            return getKeysS3(channelName, hashTime);
        } catch (Exception e) {
            logger.info("unable to find keys in S3 " + channelName + hashTime + e.getMessage());
        }
        try {
            return getKeysZookeeper(channelName, hashTime);
        } catch (Exception e) {
            logger.info("unable to find keys in ZK " + channelName + hashTime + e.getMessage());
        }
        return Collections.emptyList();
    }

    private Collection<ContentKey> getKeysZookeeper(String channelName, String hashTime) throws Exception {
        List<String> ids = curator.getChildren().forPath(TimeIndex.getPath(channelName, hashTime));
        return convertIds(ids);
    }

    private Collection<ContentKey> getKeysS3(String channelName, String hashTime) throws IOException {
        String s3Key = getS3IndexKey(channelName, hashTime);
        S3Object object = s3Client.getObject(s3BucketName, s3Key);
        byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
        List<String> ids = mapper.readValue(bytes, new TypeReference<List<String>>() { });
        return convertIds(ids);
    }

    private Collection<ContentKey> convertIds(List<String> ids) {
        List<ContentKey> keys = new ArrayList<>();
        for (String id : ids) {
            keys.add(getKey(id).get());
        }
        Collections.sort(keys);
        return keys;
    }

    private String getS3ContentKey(String channelName, ContentKey key) {
        return channelName + "/content/" + key.keyToString();
    }

    private String getS3IndexKey(String channelName, String dateTime) {
        return channelName + "/index/" + dateTime;
    }

    private void initialize() {
        logger.info("checking if bucket exists " + s3BucketName);
        if (s3Client.doesBucketExist(s3BucketName)) {
            logger.info("bucket exists " + s3BucketName);
            return;
        }
        logger.error("EXITING! unable to find bucket " + s3BucketName);
        throw new RuntimeException("unable to find bucket " + s3BucketName);
    }

    @Override
    public void initializeChannel(ChannelConfiguration config) {
        keyGenerator.seedChannel(config.getName());
    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return keyGenerator.parse(id);
    }

    @Override
    public void delete(String channelName) {
        new Thread(new S3Deleter(channelName, s3BucketName, s3Client)).start();
        keyGenerator.delete(channelName);
        String path = TimeIndex.getPath(channelName);
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete path " + path, e);
        }
    }

    @Override
    public void updateChannel(ChannelConfiguration config) {
        //no-op
    }

}
