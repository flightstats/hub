package com.flightstats.hub.dao.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.*;
import com.flightstats.hub.metrics.MetricsService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HubS3Client {

    private final static Logger logger = LoggerFactory.getLogger(HubS3Client.class);

    @Inject
    private AmazonS3 s3Client;

    @Inject
    private MetricsService metricsService;

    @Inject
    private S3BucketName s3BucketName;

    public HubS3Client(S3BucketName s3BucketName, AmazonS3 s3Client, MetricsService metricsService) {
        this.s3BucketName = s3BucketName;
        this.s3Client = s3Client;
        this.metricsService = metricsService;
    }

    public HubS3Client() {
    }

    public void initialize() {
        String bucketName = s3BucketName.getS3BucketName();
        logger.info("checking if bucket exists " + bucketName);
        if (s3Client.doesBucketExist(bucketName)) {
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
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:initiateMultipartUpload", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    UploadPartResult uploadPart(UploadPartRequest request) {
        try {
            return s3Client.uploadPart(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:uploadPart", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        try {
            return s3Client.completeMultipartUpload(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:completeMultipartUpload", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    void abortMultipartUpload(AbortMultipartUploadRequest request) {
        try {
            s3Client.abortMultipartUpload(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:abortMultipartUpload", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    S3Object getObject(GetObjectRequest request) {
        try {
            return s3Client.getObject(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:getObject", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    void deleteObject(DeleteObjectRequest request) {
        try {
            s3Client.deleteObject(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:deleteObject", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) {
        try {
            return s3Client.deleteObjects(request);
        } catch (SdkClientException e) {
            List<String> tags = new ArrayList<>();
            tags.add("exception:" + e.getClass().getCanonicalName());
            tags.add("method:deleteObjects");
            tags.add("bucket:" + request.getBucketName());
            List<DeleteObjectsRequest.KeyVersion> keyVersions = request.getKeys();
            List<String> keys = keyVersions.stream().map(DeleteObjectsRequest.KeyVersion::getKey).collect(Collectors.toList());
            tags.addAll(keys);
            metricsService.count("s3.error", 1, tags.stream().toArray(String[]::new));
            throw e;
        }
    }

    ObjectListing listObjects(ListObjectsRequest request) {
        try {
            return s3Client.listObjects(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:listObjects", "bucket:" + request.getBucketName(), "key:" + request.getPrefix());
            throw e;
        }
    }

    PutObjectResult putObject(PutObjectRequest request) {
        try {
            return s3Client.putObject(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:putObject", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    void setBucketLifecycleConfiguration(SetBucketLifecycleConfigurationRequest request) {
        try {
            s3Client.setBucketLifecycleConfiguration(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:setBucketLifecycleConfiguration", "bucket:" + request.getBucketName());
            throw e;
        }
    }

    S3ResponseMetadata getCachedResponseMetadata(CompleteMultipartUploadRequest request) {
        return s3Client.getCachedResponseMetadata(request);
    }

    S3ResponseMetadata getCachedResponseMetadata(GetObjectRequest request) {
        return s3Client.getCachedResponseMetadata(request);
    }
}
