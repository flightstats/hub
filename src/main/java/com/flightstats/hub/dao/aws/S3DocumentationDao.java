package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.*;
import com.flightstats.hub.dao.DocumentationDao;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

public class S3DocumentationDao implements DocumentationDao {

    private final static Logger logger = LoggerFactory.getLogger(S3DocumentationDao.class);

    @Inject
    private S3BucketName s3BucketName;

    @Inject
    private HubS3Client s3Client;

    @Override
    public String get(String channel) {
        logger.trace("getting documentation for channel {}", channel);
        String key = buildS3Key(channel);
        GetObjectRequest request = new GetObjectRequest(s3BucketName.getS3BucketName(), key);
        try (S3Object object = s3Client.getObject(request)) {
            byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
            return (isCompressed(object)) ? new String(decompress(bytes)) : new String(bytes);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                logger.warn("S3 exception thrown", e);
            }
            return null;
        } catch (Exception e) {
            logger.error("couldn't read object data", e);
            return null;
        }
    }

    private String buildS3Key(String channel) {
        return channel + "/documentation";
    }

    private boolean isCompressed(S3Object object) {
        ObjectMetadata metadata = object.getObjectMetadata();
        Map<String, String> userData = metadata.getUserMetadata();
        String value = userData.get("compressed");
        return value != null && !value.equals("false");
    }

    private byte[] decompress(byte[] compressedBytes) throws IOException {
        ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(compressedBytes));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = zipStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }

    @Override
    public boolean upsert(String channel, byte[] bytes) {
        String key = buildS3Key(channel);
        String bucket = s3BucketName.getS3BucketName();
        logger.trace("uploading {} bytes to {}:{}", bytes.length, bucket, key);
        try {
            InputStream stream = new ByteArrayInputStream(bytes);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.addUserMetadata("compressed", "false");
            metadata.setContentType("text/plain");
            metadata.setContentLength(bytes.length);
            PutObjectRequest request = new PutObjectRequest(bucket, key, stream, metadata);
            s3Client.putObject(request);
            return true;
        } catch (AmazonS3Exception e) {
            logger.error("unable to write to " + key, e);
            return false;
        }
    }

    @Override
    public boolean delete(String channel) {
        String key = buildS3Key(channel);
        String bucket = s3BucketName.getS3BucketName();
        logger.trace("deleting documentation for {}", channel);
        try {
            DeleteObjectRequest request = new DeleteObjectRequest(bucket, key);
            s3Client.deleteObject(request);
            return true;
        } catch (AmazonS3Exception e) {
            logger.error("unable to delete " + key, e);
            return false;
        }
    }
}
