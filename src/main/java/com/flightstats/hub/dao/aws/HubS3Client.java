package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.S3ResponseMetadata;
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
import com.flightstats.hub.config.properties.S3Properties;
import com.flightstats.hub.metrics.StatsdReporter;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class HubS3Client {

    private final AmazonS3 s3Client;
    private final StatsdReporter statsdReporter;
    private final String bucketName;

    @Inject
    public HubS3Client(S3Properties s3Properties, AmazonS3 s3Client, StatsdReporter statsdReporter) {
        this.bucketName = s3Properties.getBucketName();
        this.s3Client = s3Client;
        this.statsdReporter = statsdReporter;
    }

    private static String[] toStringArray(List<String> list) {
        return list.toArray(new String[0]);
    }

    public void initialize() {
        log.debug("checking if bucket exists {}", bucketName);
        if (s3Client.doesBucketExistV2(bucketName)) {
            log.debug("bucket exists {}", bucketName);
            return;
        }
        log.error("EXITING! unable to find bucket " + bucketName);
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

    void countError(SdkClientException exception, AmazonWebServiceRequest request, String method, List<String> extraTags) {
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
}
