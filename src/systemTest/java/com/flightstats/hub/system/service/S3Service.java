package com.flightstats.hub.system.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;
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
    public byte[] getS3BatchedItems(String path) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, path);
        S3Object obj = s3Client.getObject(getObjectRequest);
        try (S3ObjectInputStream content = obj.getObjectContent()) {
            return handleZip(content);
        }
    }

    @SneakyThrows
    private byte[] handleZip(InputStream read) {
        try (ZipInputStream zipStream = new ZipInputStream(read)) {
            byte [] buffer = new byte[zipStream.available()];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (zipStream.getNextEntry() != null) {
                while (zipStream.read(buffer) > 0) {
                    bos.write(buffer, 0, buffer.length);
                }
            }
            return bos.toByteArray();
        }
    }

    private String stripSecondsAndKey(String path) {
        String [] pathParts = path.split("/");
        String newPath =  Arrays.asList(pathParts)
                .subList(0, 5)
                .stream()
                .map(str-> str.concat("/"))
                .collect(Collectors.joining());
                return newPath.substring(0, newPath.length() - 1);
    }

    public String formatS3BatchItemPath(String fullPath, String channelName) {
        String base = fullPath != null ? fullPath
                .replace(hubBaseUrl, "")
                .replace("/channel/", "")
                .replace(bucketName, "")
                .replace(channelName + "/", ""): "";
        return channelName + "Batch/items/" + stripSecondsAndKey(base);
    }
}
