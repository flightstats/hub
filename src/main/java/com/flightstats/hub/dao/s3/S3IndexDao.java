package com.flightstats.hub.dao.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.dao.timeIndex.TimeIndexDao;
import com.flightstats.hub.model.ContentKey;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class S3IndexDao implements TimeIndexDao {
    private final static Logger logger = LoggerFactory.getLogger(S3IndexDao.class);

    private final AmazonS3 s3Client;
    private final String s3BucketName;
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public S3IndexDao(AmazonS3 s3Client, S3BucketName s3BucketName) {
        this.s3Client = s3Client;
        this.s3BucketName = s3BucketName.getS3BucketName();
    }

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

    Collection<ContentKey> getKeys(String channelName, String hashTime) throws IOException {
        String s3Key = getS3IndexKey(channelName, hashTime);
        S3Object object = s3Client.getObject(s3BucketName, s3Key);
        byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
        List<String> ids = mapper.readValue(bytes, new TypeReference<List<String>>() { });
        return IndexUtils.convertIds(ids);
    }

    private String getS3IndexKey(String channelName, String dateTime) {
        return channelName + "/index/" + dateTime;
    }
}
