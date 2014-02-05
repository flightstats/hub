package com.flightstats.hub.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.timeIndex.TimeIndex;
import com.flightstats.hub.dao.timeIndex.TimeIndexDao;
import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.metrics.TimedCallback;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ValueInsertionResult;
import com.flightstats.hub.util.ContentKeyGenerator;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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

    @Inject
    public S3ContentDao(ContentKeyGenerator keyGenerator,
                        AmazonS3 s3Client,
                        @Named("aws.environment") String environment,
                        CuratorFramework curator, MetricsTimer metricsTimer) {
        this.keyGenerator = keyGenerator;
        this.s3Client = s3Client;
        this.curator = curator;
        this.metricsTimer = metricsTimer;
        this.s3BucketName = "deihub-" + environment;
    }

    @Override
    public ValueInsertionResult write(String channelName, Content content, long ttlDays) {
        if (!content.getContentKey().isPresent()) {
            content.setContentKey(keyGenerator.newKey(channelName));
        }
        ContentKey key = content.getContentKey().get();
        DateTime dateTime = new DateTime(content.getMillis());
        writeS3(channelName, content, key);
        writeIndex(channelName, dateTime, key);
        return new ValueInsertionResult(key, dateTime.toDate());
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
    public Content read(String channelName, ContentKey key) {
        try {
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
        } catch (AmazonClientException e) {
            logger.info("unable to get " + channelName + " " + key.keyToString() + " " + e.getMessage());
        } catch (IOException e) {
            logger.warn("unable to get " + channelName + " " + key.keyToString(), e);
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

    @Override
    public void initialize() {
        if (s3Client.doesBucketExist(s3BucketName)) {
            logger.info("bucket exists " + s3BucketName);
            return;
        }
        logger.info("creating " + s3BucketName);
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(s3BucketName, Region.US_Standard);
        Bucket bucket = s3Client.createBucket(createBucketRequest);
        logger.info("created " + bucket);
    }

    @Override
    public void initializeChannel(ChannelConfiguration config) {
        keyGenerator.seedChannel(config.getName());
        modifyLifeCycle(config);
    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return keyGenerator.parse(id);
    }

    @Override
    public void delete(String channelName) {
        //todo - gfm - 1/19/14 - remove LifeCycle config - or do this with the metadata
        //todo - gfm - 1/19/14 - this could be more sophisticated, making sure the request gets picked up if this server
        //goes down
        new Thread(new S3Deleter(channelName, s3BucketName, s3Client)).start();
    }

    @Override
    public void updateChannel(ChannelConfiguration config) {
        modifyLifeCycle(config);
    }

    private void modifyLifeCycle(ChannelConfiguration config) {
        //todo - gfm - 1/7/14 - this should happen in an system wide lock on ChannelConfig
        // or it should be triggered occasionally via ChannelMetadata
        // or create a new bucket per channel

        /*BucketLifecycleConfiguration lifecycleConfig = s3Client.getBucketLifecycleConfiguration(s3BucketName);
        logger.info("found config " + lifecycleConfig);
        String namePrefix = config.getName() + "/";
        BucketLifecycleConfiguration.Rule newRule = new BucketLifecycleConfiguration.Rule()
                .withPrefix(namePrefix)
                .withId(config.getName())
                .withExpirationInDays((int) config.getTtlDays())
                .withStatus(BucketLifecycleConfiguration.ENABLED);
        if (lifecycleConfig == null) {
            ArrayList<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();
            rules.add(newRule);
            lifecycleConfig = new BucketLifecycleConfiguration(rules);
        } else {
            BucketLifecycleConfiguration.Rule toRemove = null;
            List<BucketLifecycleConfiguration.Rule> rules = lifecycleConfig.getRules();
            for (BucketLifecycleConfiguration.Rule rule : rules) {
                if (rule.getPrefix().equals(namePrefix)) {
                    toRemove = rule;
                }
            }
            if (toRemove != null) {
                logger.info("removing rule " + toRemove.getPrefix());
                rules.remove(toRemove);
            }
            rules.add(newRule);
        }

        s3Client.setBucketLifecycleConfiguration(s3BucketName, lifecycleConfig);*/
    }


}
