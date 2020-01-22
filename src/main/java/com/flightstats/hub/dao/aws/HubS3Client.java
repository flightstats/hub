package com.flightstats.hub.dao.aws;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.GetBucketLifecycleConfigurationRequest;
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
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
            countError(e, request, "initiateMultipartUpload", Collections.singletonList(request.getKey()));
            throw e;
        }
    }

    UploadPartResult uploadPart(UploadPartRequest request) {
        try {
            return s3Client.uploadPart(request);
        } catch (SdkClientException e) {
            countError(e, request, "uploadPart", Collections.singletonList(request.getKey()));
            throw e;
        }
    }

    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        try {
            return s3Client.completeMultipartUpload(request);
        } catch (SdkClientException e) {
            countError(e, request, "completeMultipartUpload", Collections.singletonList(request.getKey()));
            throw e;
        }
    }

    void abortMultipartUpload(AbortMultipartUploadRequest request) {
        try {
            s3Client.abortMultipartUpload(request);
        } catch (SdkClientException e) {
            countError(e, request, "abortMultipartUpload", Collections.singletonList(request.getKey()));
            throw e;
        }
    }

    S3Object getObject(GetObjectRequest request) {
        try {
            return s3Client.getObject(request);
        } catch (SdkClientException e) {
            countError(e, request, "getObject", Collections.singletonList(request.getKey()));
            throw e;
        }
    }

    void deleteObject(DeleteObjectRequest request) {
        try {
            s3Client.deleteObject(request);
        } catch (SdkClientException e) {
            countError(e, request, "deleteObject", Collections.singletonList(request.getKey()));
            throw e;
        }
    }

    DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) {
        try {
            return s3Client.deleteObjects(request);
        } catch (SdkClientException e) {
            List<String> keys = request.getKeys().stream()
                    .map(DeleteObjectsRequest.KeyVersion::getKey)
                    .collect(Collectors.toList());
            countError(e, request, "deleteObjects", keys);
            throw e;
        }
    }

    ObjectListing listObjects(ListObjectsRequest request) {
        try {
            return s3Client.listObjects(request);
        } catch (SdkClientException e) {
            countError(e, request, "listObjects", Collections.singletonList(request.getPrefix()));
            throw e;
        }
    }

    PutObjectResult putObject(PutObjectRequest request) {
        try {
            return s3Client.putObject(request);
        } catch (SdkClientException e) {
            countError(e, request, "putObject", Collections.singletonList(request.getKey()));
            throw e;
        }
    }

    void setBucketLifecycleConfiguration(SetBucketLifecycleConfigurationRequest request) {
        try {
            s3Client.setBucketLifecycleConfiguration(request);
        } catch (SdkClientException e) {
            countError(e, request, "setBucketLifecycleConfiguration", Collections.emptyList());
            throw e;
        }
    }

    BucketLifecycleConfiguration getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest request) {
        try {
            return s3Client.getBucketLifecycleConfiguration(request);
        } catch (SdkClientException e) {
            countError(e, request, "getBucketLifecycleConfiguration", Collections.emptyList());
            throw e;
        }
    }

    S3ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        return s3Client.getCachedResponseMetadata(request);
    }

    void countError(SdkClientException exception, AmazonWebServiceRequest request, String method, List<String> keys) {
        List<String> tags = new ArrayList<>();
        tags.add("exception:" + exception.getClass().getCanonicalName());
        tags.add("method:" + method);
        tags.add("bucket:" + bucketName);

        String requestId = Optional.ofNullable(getCachedResponseMetadata(request))
                .map(ResponseMetadata::getRequestId)
                .orElse("unknown");
        String errorMessage = String.format("S3 Error %s for bucket %s and keys: %s. Request ID: %s",
                method,
                bucketName,
                String.join(", ", keys),
                requestId);
        log.error(errorMessage, exception);
        statsdReporter.count("s3.error", 1, toStringArray(tags));
    }
}
