package com.flightstats.hub.system.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

@Slf4j
public class S3Service {
    private final AmazonS3 s3Client;
    private final String bucketName;
    private final String hubBaseUrl;

    @Inject
    public S3Service(AmazonS3 s3Client, @Named("hub.url") String hubBaseUrl, @Named("s3.bucket.name") String bucketName) {
        this.s3Client = s3Client;
        this.hubBaseUrl = hubBaseUrl;
        this.bucketName = bucketName;
    }

    @SneakyThrows
    public byte[] getS3Item(String path) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, path);
        S3Object obj = s3Client.getObject(getObjectRequest);
        try (S3ObjectInputStream content = obj.getObjectContent()) {
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, path);
            if (metadata.getUserMetadata().containsKey("compressed")) {
                return inflate(content);
            }
            return IOUtils.toByteArray(content);
        }
    }

    @SneakyThrows
    private byte[] inflate(InputStream read) {
        try (ZipInputStream zipStream = new ZipInputStream(read)) {
            zipStream.getNextEntry();
            return IOUtils.toByteArray(zipStream);
        }
    }

    public String parseS3BatchItemsPath(String fullPath, String channelName) {
        String base = fullPath != null ? fullPath
                .replace(hubBaseUrl, "")
                .replace("/channel/", "")
                .replace(bucketName, "")
                .replace(channelName + "/", ""): "";
        String withKey = channelName + "Batch/items/" + base;
        String withoutKey = withKey.substring(0, withKey.lastIndexOf("/"));
        String noMillis = withoutKey.substring(0, withoutKey.lastIndexOf("/"));
        return noMillis.substring(0, noMillis.lastIndexOf("/"));
    }
}
