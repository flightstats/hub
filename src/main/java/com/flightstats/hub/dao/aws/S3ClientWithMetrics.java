package com.flightstats.hub.dao.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.flightstats.hub.metrics.MetricsService;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class S3ClientWithMetrics {

    @Inject
    private static AmazonS3 s3Client;

    @Inject
    private static MetricsService metricsService;

    static InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        try {
            return s3Client.initiateMultipartUpload(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:initiateMultipartUpload", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    static UploadPartResult uploadPart(UploadPartRequest request) {
        try {
            return s3Client.uploadPart(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:uploadPart", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    static CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        try {
            return s3Client.completeMultipartUpload(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:completeMultipartUpload", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    static void abortMultipartUpload(AbortMultipartUploadRequest request) {
        try {
            s3Client.abortMultipartUpload(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:abortMultipartUpload", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    static S3Object getObject(GetObjectRequest request) {
        try {
            return s3Client.getObject(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:getObject", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    static void deleteObject(DeleteObjectRequest request) {
        try {
            s3Client.deleteObject(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:deleteObject", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    static DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) {
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

    static ObjectListing listObjects(ListObjectsRequest request) {
        try {
            return s3Client.listObjects(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:listObjects", "bucket:" + request.getBucketName(), "key:" + request.getPrefix());
            throw e;
        }
    }

    static PutObjectResult putObject(PutObjectRequest request) {
        try {
            return s3Client.putObject(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:putObject", "bucket:" + request.getBucketName(), "key:" + request.getKey());
            throw e;
        }
    }

    static void setBucketLifecycleConfiguration(SetBucketLifecycleConfigurationRequest request) {
        try {
            s3Client.setBucketLifecycleConfiguration(request);
        } catch (SdkClientException e) {
            metricsService.count("s3.error", 1, "exception:" + e.getClass().getCanonicalName(), "method:setBucketLifecycleConfiguration", "bucket:" + request.getBucketName());
            throw e;
        }
    }
}
