package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.*;
import com.flightstats.hub.metrics.StatsdReporter;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HubS3Client {

    private final static Logger logger = LoggerFactory.getLogger(HubS3Client.class);

    @Inject
    private AmazonS3 s3Client;

    @Inject
    private StatsdReporter statsdReporter;

    @Inject
    private S3BucketName s3BucketName;

    public HubS3Client(S3BucketName s3BucketName, AmazonS3 s3Client, StatsdReporter statsdReporter) {
        this.s3BucketName = s3BucketName;
        this.s3Client = s3Client;
        this.statsdReporter = statsdReporter;
    }

    public HubS3Client() {
    }

    public void initialize() {
        String bucketName = s3BucketName.getS3BucketName();
        logger.info("checking if bucket exists " + bucketName);
        if (s3Client.doesBucketExistV2(bucketName)) {
            logger.info("bucket exists " + bucketName);
            return;
        }
        logger.error("EXITING! unable to find bucket " + bucketName);
        throw new RuntimeException("unable to find bucket " + bucketName);
    }

    InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        try {
            return s3Client.initiateMultipartUpload(request);
        } catch (SdkClientException e) {
            countError(e, request, "initiateMultipartUpload", Arrays.asList("bucket:" + request.getBucketName(), "key:" + request.getKey()));
            throw e;
        }
    }

    UploadPartResult uploadPart(UploadPartRequest request) {
        try {
            return s3Client.uploadPart(request);
        } catch (SdkClientException e) {
            countError(e, request, "uploadPart", Arrays.asList("bucket:" + request.getBucketName(), "key:" + request.getKey()));
            throw e;
        }
    }

    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        try {
            return s3Client.completeMultipartUpload(request);
        } catch (SdkClientException e) {
            countError(e, request, "completeMultipartUpload", Arrays.asList("bucket:" + request.getBucketName(), "key:" + request.getKey()));
            throw e;
        }
    }

    void abortMultipartUpload(AbortMultipartUploadRequest request) {
        try {
            s3Client.abortMultipartUpload(request);
        } catch (SdkClientException e) {
            countError(e, request, "abortMultipartUpload", Arrays.asList("bucket:" + request.getBucketName(), "key:" + request.getKey()));
            throw e;
        }
    }

    S3Object getObject(GetObjectRequest request) {
        try {
            return s3Client.getObject(request);
        } catch (SdkClientException e) {
            countError(e, request, "getObject", Arrays.asList("bucket:" + request.getBucketName(), "key:" + request.getKey()));
            throw e;
        }
    }

    void deleteObject(DeleteObjectRequest request) {
        try {
            s3Client.deleteObject(request);
        } catch (SdkClientException e) {
            countError(e, request, "deleteObject", Arrays.asList("bucket:" + request.getBucketName(), "key:" + request.getKey()));
            throw e;
        }
    }

    DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) {
        try {
            return s3Client.deleteObjects(request);
        } catch (SdkClientException e) {
            List<String> tags = new ArrayList<>();
            tags.add("bucket:" + request.getBucketName());
            List<String> keys = request.getKeys().stream()
                    .map((keyVersion) -> "key:" + keyVersion.getKey())
                    .collect(Collectors.toList());
            tags.addAll(keys);
            countError(e, request, "deleteObjects", tags);
            throw e;
        }
    }

    ObjectListing listObjects(ListObjectsRequest request) {
        try {
            return s3Client.listObjects(request);
        } catch (SdkClientException e) {
            countError(e, request, "listObjects", Arrays.asList("bucket:" + request.getBucketName(), "key:" + request.getPrefix()));
            throw e;
        }
    }

    PutObjectResult putObject(PutObjectRequest request) {
        try {
            return s3Client.putObject(request);
        } catch (SdkClientException e) {
            countError(e, request, "putObject", Arrays.asList("bucket:" + request.getBucketName(), "key:" + request.getKey()));
            throw e;
        }
    }

    void setBucketLifecycleConfiguration(SetBucketLifecycleConfigurationRequest request) {
        try {
            s3Client.setBucketLifecycleConfiguration(request);
        } catch (SdkClientException e) {
            countError(e, request, "setBucketLifecycleConfiguration", Collections.singletonList("bucket:" + request.getBucketName()));
            throw e;
        }
    }

    S3ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        return s3Client.getCachedResponseMetadata(request);
    }

    @VisibleForTesting
    protected void countError(SdkClientException exception, AmazonWebServiceRequest request, String method, List<String> extraTags) {
        List<String> tags = new ArrayList<>();
        tags.add("exception:" + exception.getClass().getCanonicalName());
        tags.add("method:" + method);

        S3ResponseMetadata metadata = getCachedResponseMetadata(request);
        if (metadata != null) {
            tags.add("requestId:" + metadata.getRequestId());
        }

        tags.addAll(extraTags);
        statsdReporter.count("s3.error", 1, toStringArray(tags));
    }

    private static String[] toStringArray(List<String> list) {
        return list.toArray(new String[0]);
    }
}
