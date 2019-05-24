package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.dao.DocumentationDao;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Slf4j
public class S3DocumentationDao implements DocumentationDao {

    private final HubS3Client s3Client;
    private final String bucketName;

    @Inject
    public S3DocumentationDao(HubS3Client s3Client, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.bucketName = s3Properties.getBucketName();
    }

    @Override
    public String get(String channel) {
        log.trace("getting documentation for channel {}", channel);
        String key = buildS3Key(channel);
        GetObjectRequest request = new GetObjectRequest(bucketName, key);
        try (S3Object object = s3Client.getObject(request)) {
            byte[] bytes = ByteStreams.toByteArray(object.getObjectContent());
            return (isCompressed(object)) ? new String(decompress(bytes)) : new String(bytes);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                log.warn("S3 exception thrown", e);
            }
            return null;
        } catch (Exception e) {
            log.error("couldn't read object data", e);
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
        log.trace("uploading {} bytes to {}:{}", bytes.length, bucketName, key);
        try {
            InputStream stream = new ByteArrayInputStream(bytes);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.addUserMetadata("compressed", "false");
            metadata.setContentType("text/plain");
            metadata.setContentLength(bytes.length);
            PutObjectRequest request = new PutObjectRequest(bucketName, key, stream, metadata);
            s3Client.putObject(request);
            return true;
        } catch (AmazonS3Exception e) {
            log.error("unable to write to " + key, e);
            return false;
        }
    }

    @Override
    public boolean delete(String channel) {
        String key = buildS3Key(channel);
        log.trace("deleting documentation for {}", channel);
        try {
            DeleteObjectRequest request = new DeleteObjectRequest(bucketName, key);
            s3Client.deleteObject(request);
            return true;
        } catch (AmazonS3Exception e) {
            log.error("unable to delete " + key, e);
            return false;
        }
    }
}
