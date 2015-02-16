package com.flightstats.hub.dao.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import org.junit.BeforeClass;
import org.junit.Test;

public class S3ContentDaoTest {

    private static ContentDaoUtil util;

    @BeforeClass
    public static void setUpClass() throws Exception {
        HubProperties.loadProperties("useDefault");
        AwsConnectorFactory factory = new AwsConnectorFactory();
        AmazonS3 s3Client = factory.getS3Client();
        S3BucketName bucketName = new S3BucketName("local", "hub-v2");
        S3ContentDao s3ContentDao = new S3ContentDao(s3Client, false, bucketName, 3);
        util = new ContentDaoUtil(s3ContentDao);
    }

    @Test
    public void testWriteRead() throws Exception {
        util.testWriteRead();
    }

    @Test
    public void testWriteReadNoOptionals() throws Exception {
        util.testWriteReadNoOptionals();
    }

    @Test
    public void testQueryRangeDay() throws Exception {
        util.testQueryRangeDay();
    }

    @Test
    public void testQueryRangeHour() throws Exception {
        util.testQueryRangeHour();
    }

    @Test
    public void testQueryRangeMinute() throws Exception {
        util.testQueryRangeMinute();
    }

    @Test
    public void testDirectionQuery() throws Exception {
        util.testDirectionQuery();
    }

}