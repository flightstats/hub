package com.flightstats.datahub.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.flightstats.datahub.dao.ContentDao;
import com.flightstats.datahub.dao.TimeIndex;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This uses S3 for Content and ZooKeeper for TimeIndex
 */
public class S3ContentDao implements ContentDao, TimeIndexDao {

    private final static Logger logger = LoggerFactory.getLogger(S3ContentDao.class);

    private final DataHubKeyGenerator keyGenerator;
    private final AmazonS3 s3Client;
    private final CuratorFramework curator;
    private final String s3BucketName;

    @Inject
    public S3ContentDao(DataHubKeyGenerator keyGenerator,
                        AmazonS3 s3Client,
                        @Named("dynamo.environment") String environment,
                        CuratorFramework curator) {
        this.keyGenerator = keyGenerator;
        this.s3Client = s3Client;
        this.curator = curator;
        this.s3BucketName = "deihub-" + environment;
    }

    @Override
    public ValueInsertionResult write(String channelName, Content content, Optional<Integer> ttlSeconds) {
        //todo - gfm - 1/3/14 - what happens if one or the other fails?
        ContentKey key = keyGenerator.newKey(channelName);
        DateTime dateTime = new DateTime();
        writeS3(channelName, content, key);
        writeIndex(channelName, dateTime, key);
        return new ValueInsertionResult(key, dateTime.toDate());
    }

    public void writeIndex(String channelName, DateTime dateTime, ContentKey key) {
        try {
            String path = TimeIndex.getPath(channelName, dateTime, key);
            //todo - gfm - 1/6/14 - remove this
            logger.info("writing " + path);
            curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
        } catch (Exception e) {
            logger.warn("unable to create", e);
            throw new RuntimeException(e);
        }
    }

    private void writeS3(String channelName, Content content, ContentKey key) {
        String s3Key = getS3Key(channelName, key);
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

        PutObjectRequest request = new PutObjectRequest(s3BucketName, s3Key, stream, metadata);
        s3Client.putObject(request);
    }

    @Override
    public void writeIndices(String channelName, String dateTime, List<String> keys) {
        //todo - gfm - 1/6/14 -
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private String getS3Key(String channelName, ContentKey key) {
        return channelName + "/" + key.keyToString();
    }

    @Override
    public Content read(String channelName, ContentKey key) {
        try {
            S3Object object = s3Client.getObject(s3BucketName, getS3Key(channelName, key));
            S3ObjectInputStream objectContent = object.getObjectContent();
            byte[] bytes = ByteStreams.toByteArray(objectContent);
            ObjectMetadata metadata = object.getObjectMetadata();
            Map<String, String> userData = metadata.getUserMetadata();
            String type = userData.get("type");
            Optional<String> contentType = Optional.absent();
            if (!type.equals("none")) {
                contentType = Optional.of(type);
            }
            String language = userData.get("language");
            Optional<String> contentLang = Optional.absent();
            if (!language.equals("none")) {
                contentLang = Optional.of(language);
            }
            Date lastModified = metadata.getLastModified();
            return new Content(contentType, contentLang, bytes, lastModified.getTime());
        } catch (AmazonClientException e) {
            logger.info("unable to get " + channelName + " " + key.keyToString() + " " + e.getMessage());
        } catch (IOException e) {
            logger.warn("unable to get " + channelName + " " + key.keyToString(), e);
        }
        return null;
    }

    @Override
    public void initialize() {
        try {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(s3BucketName).withMaxKeys(0);
            s3Client.listObjects(listObjectsRequest);
            logger.info("bucket exists " + s3BucketName);
        } catch (AmazonClientException e) {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(s3BucketName, Region.US_Standard);
            Bucket bucket = s3Client.createBucket(createBucketRequest);
            logger.info("created " + bucket);
        }
    }

    @Override
    public void initializeChannel(ChannelConfiguration config) {

        keyGenerator.seedChannel(config.getName());

        //todo - gfm - 1/3/14 - how do we set config policies for S3 bucket prefixes?
    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return keyGenerator.parse(id);
    }

    @Override
    public Iterable<ContentKey> getKeys(ChannelConfiguration configuration, DateTime dateTime) {
        //todo - gfm - 1/6/14 - first look in S3, then check ZK

        return null;
    }

    @Override
    public void delete(String channelName) {
        //todo - gfm - 1/6/14 -
    }

    @Override
    public void updateChannel(ChannelConfiguration configuration) {
        //todo - gfm - 1/6/14 -
    }


}
