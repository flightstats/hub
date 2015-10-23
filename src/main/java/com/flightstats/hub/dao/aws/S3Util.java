package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Util {

    private final static Logger logger = LoggerFactory.getLogger(S3Util.class);

    public static void initialize(String s3BucketName, AmazonS3 s3Client) {
        logger.info("checking if bucket exists " + s3BucketName);
        if (s3Client.doesBucketExist(s3BucketName)) {
            logger.info("bucket exists " + s3BucketName);
            return;
        }
        logger.error("EXITING! unable to find bucket " + s3BucketName);
        throw new RuntimeException("unable to find bucket " + s3BucketName);
    }
}
