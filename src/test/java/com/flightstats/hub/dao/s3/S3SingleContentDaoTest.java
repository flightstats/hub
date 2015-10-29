package com.flightstats.hub.dao.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.dao.aws.AwsConnectorFactory;
import com.flightstats.hub.dao.aws.S3BucketName;
import com.flightstats.hub.dao.aws.S3SingleContentDao;
import com.flightstats.hub.metrics.NoOpMetricsSender;
import org.junit.BeforeClass;
import org.junit.Test;

public class S3SingleContentDaoTest {

    private static ContentDaoUtil util;

    @BeforeClass
    public static void setUpClass() throws Exception {
        HubProperties.loadProperties("useDefault");
        AwsConnectorFactory factory = new AwsConnectorFactory();
        AmazonS3 s3Client = factory.getS3Client();
        S3BucketName bucketName = new S3BucketName("local", "hub-v2");
        S3SingleContentDao s3SingleContentDao = new S3SingleContentDao(s3Client, bucketName, new NoOpMetricsSender());
        util = new ContentDaoUtil(s3SingleContentDao);
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

    @Test
    public void testDelete() throws Exception {
        util.testDeleteMaxItems();
    }

}