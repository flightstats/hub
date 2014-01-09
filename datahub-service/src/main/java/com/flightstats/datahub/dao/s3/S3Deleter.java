package com.flightstats.datahub.dao.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a first go, does not handle restarts
 */
public class S3Deleter implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(S3Deleter.class);

    private String channelName;
    private String bucketName;
    private AmazonS3 s3Client;

    public S3Deleter(String channelName, String bucketName, AmazonS3 s3Client) {
        this.channelName = channelName + "/";
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }

    @Override
    public void run() {
        while (delete()) {
            logger.info("deleting more from " + channelName);
        }
        delete();
        logger.info("completed deletion of " + channelName);
    }

    private boolean delete() {
        ListObjectsRequest request = new ListObjectsRequest();
        request.withBucketName(bucketName);
        request.withPrefix(channelName);
        ObjectListing listing = s3Client.listObjects(request);
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        List<S3ObjectSummary> objectSummaries = listing.getObjectSummaries();
        for (S3ObjectSummary objectSummary : objectSummaries) {

            keys.add(new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()));
        }
        DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(bucketName);
        multiObjectDeleteRequest.setKeys(keys);

        try {
            s3Client.deleteObjects(multiObjectDeleteRequest);

        } catch (MultiObjectDeleteException e) {
            logger.info("what happened? " + channelName, e);
        }
        return listing.isTruncated();
    }
}
