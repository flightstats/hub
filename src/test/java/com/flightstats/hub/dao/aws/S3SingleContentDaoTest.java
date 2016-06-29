package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.metrics.NoOpMetricsSender;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class S3SingleContentDaoTest {

    private final static Logger logger = LoggerFactory.getLogger(S3SingleContentDaoTest.class);

    private static ContentDaoUtil util;
    private static S3SingleContentDao s3SingleContentDao;

    @BeforeClass
    public static void setUpClass() throws Exception {
        HubProperties.loadProperties("useDefault");
        AwsConnectorFactory factory = new AwsConnectorFactory();
        AmazonS3 s3Client = factory.getS3Client();
        S3BucketName bucketName = new S3BucketName("local", "hub-v2");
        s3SingleContentDao = new S3SingleContentDao(s3Client, bucketName, new NoOpMetricsSender());
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
    public void testQuery15Minutes() throws Exception {
        util.testQuery15Minutes();
    }

    @Test
    public void testDirectionQuery() throws Exception {
        util.testDirectionQuery();
    }

    @Test
    public void testDelete() throws Exception {
        util.testDeleteMaxItems();
    }

    @Test
    public void testWriteReadOld() throws Exception {
        String channel = "testWriteReadOld";
        Content content = util.createContent();
        ContentKey key = s3SingleContentDao.insertOld(channel, content);
        logger.info("key {}", key);
        assertEquals(content.getContentKey().get(), key);
        Content read = s3SingleContentDao.get(channel, key);
        logger.info("read {}", read.getContentKey());
        util.compare(content, read, key.toString().getBytes());
    }

}