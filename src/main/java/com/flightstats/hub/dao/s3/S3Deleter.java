package com.flightstats.hub.dao.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a first go, does not handle restarts
 * todo - gfm - 1/21/14 - handle restarts
 * todo - gfm - 1/21/14 - this does not seem to delete all of the data.
 */
public class S3Deleter implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(S3Deleter.class);

    private String channelName;
    private String bucketName;
    private AmazonS3 s3Client;
    private long deleted = 0;

    public S3Deleter(String channelName, String bucketName, AmazonS3 s3Client) {
        this.channelName = channelName + "/";
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }

    @Override
    public void run() {
        try {
            while (delete()) {
                logger.info("deleting more from " + channelName + " deleted " + deleted);
            }
            delete();
            logger.info("completed deletion of " + channelName + " deleted " + deleted + " items");
        } catch (Exception e) {
            logger.warn("unable to delete " + channelName + " in " + bucketName, e);
        }
    }

    private boolean delete() {
        ListObjectsRequest request = new ListObjectsRequest();
        request.withBucketName(bucketName);
        request.withPrefix(channelName);
        ObjectListing listing = s3Client.listObjects(request);
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (S3ObjectSummary objectSummary : listing.getObjectSummaries()) {
            keys.add(new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()));
        }
        if (keys.isEmpty()) {
            return false;
        }
        DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(bucketName);
        multiObjectDeleteRequest.setKeys(keys);
        try {
            s3Client.deleteObjects(multiObjectDeleteRequest);
            deleted += keys.size();
        } catch (MultiObjectDeleteException e) {
            logger.info("what happened? " + channelName, e);
            return true;
        }
        return listing.isTruncated();
    }
}
