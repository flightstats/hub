package com.flightstats.datahub.dao.dynamo;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.flightstats.datahub.dao.ContentDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

/**
 * todo - gfm - 1/5/14 - flush this out
 */
public class S3ContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(S3ContentDao.class);

    private final DataHubKeyGenerator keyGenerator;
    private final AmazonS3 s3Client;
    private final TransferManager transferManager;

    @Inject
    public S3ContentDao(DataHubKeyGenerator keyGenerator,
                        AmazonS3 s3Client) {
        this.keyGenerator = keyGenerator;
        this.s3Client = s3Client;
        transferManager = new TransferManager(s3Client);

    }

    @Override
    public ValueInsertionResult write(String channelName, Content content, Optional<Integer> ttlSeconds) {
        //todo - gfm - 1/3/14 - transferManager.upload()


        ContentKey key = keyGenerator.newKey(channelName);
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

        PutObjectRequest request = new PutObjectRequest(getBucketName(), s3Key, stream, metadata);
        Upload upload = transferManager.upload(request);
        //PutObjectResult result = s3Client.putObject(request);
        return new ValueInsertionResult(key, new Date());
    }

    private String getS3Key(String channelName, ContentKey key) {
        return channelName + "/" + key.keyToString();
    }

    @Override
    public Content read(String channelName, ContentKey key) {

        try {
            S3Object object = s3Client.getObject(getBucketName(), getS3Key(channelName, key));
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
        /**
         * aused by: com.amazonaws.services.s3.model.AmazonS3Exception: Status Code: 409, AWS Service: Amazon S3, AWS Request ID: 9A80DAEE9CA978F6, AWS Error Code: OperationAborted, AWS Error Message: A conflicting conditional operation is currently in progress against this resource. Please try again., S3 Extended Request ID: DZd/0pAONr/LLrAmOn3ukD4qaUhV30nE2iJ/VJAFpsAvxIWu9KMIO2hawAWnruzaAphVdjrdxt8=
         at com.amazonaws.http.AmazonHttpClient.handleErrorResponse(AmazonHttpClient.java:767)
         at com.amazonaws.http.AmazonHttpClient.executeHelper(AmazonHttpClient.java:414)
         at com.amazonaws.http.AmazonHttpClient.execute(AmazonHttpClient.java:228)
         at com.amazonaws.services.s3.AmazonS3Client.invoke(AmazonS3Client.java:3214)
         at com.amazonaws.services.s3.AmazonS3Client.createBucket(AmazonS3Client.java:704)
         at com.flightstats.datahub.dao.dynamo.S3ContentDao.initialize(S3ContentDao.java:102)

         If you have to check the existence of a bucket, do so by making a ListBucket or HEAD request on the bucket,
         specifying the max-keys query-string parameter as 0. In REST, this would translate to the URL
         http://bucket.s3.amazonaws.com/?max-keys=0 with the appropriate signature. Avoid using bucket PUTs to test bucket existence.

         */
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(getBucketName(), Region.US_Standard);
        Bucket bucket = s3Client.createBucket(createBucketRequest);
        logger.info("created " + bucket);
    }

    private String getBucketName() {
        return "deihub-dev";
    }

    @Override
    public void initializeChannel(ChannelConfiguration config) {

        //todo - gfm - 1/3/14 - how do we set config policies?
        keyGenerator.seedChannel(config.getName());
    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return keyGenerator.parse(id);
    }

    @Override
    public Iterable<ContentKey> getKeys(ChannelConfiguration configuration, DateTime dateTime) {
        return null;
    }

    @Override
    public void delete(String channelName) {

    }

    @Override
    public void updateChannel(ChannelConfiguration configuration) {

    }


}
