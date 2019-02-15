package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class S3SingleContentDaoTest {

    private final static Logger logger = LoggerFactory.getLogger(S3SingleContentDaoTest.class);

    private static ContentDaoUtil util;
    private static S3SingleContentDao s3SingleContentDao;

    @BeforeAll
    public static void setUpClass() throws Exception {
        HubProperties.loadProperties("useDefault");
        Injector injector = Integration.startAwsHub();
        s3SingleContentDao = injector.getInstance(S3SingleContentDao.class);
        util = new ContentDaoUtil(s3SingleContentDao);
    }

    @Test
    public void testWriteRead() throws Exception {
        util.testWriteRead(ContentDaoUtil.createContent());
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
    public void testPreviousFromBulk_Issue753() throws Exception {
        util.testPreviousFromBulk_Issue753();
    }

    @Test
    public void testWriteReadOld() throws Exception {
        String channel = "testWriteReadOld";
        Content content = ContentDaoUtil.createContent();
        ContentKey key = s3SingleContentDao.insertOld(channel, content);
        logger.info("key {}", key);
        assertEquals(content.getContentKey().get(), key);
        Content read = s3SingleContentDao.get(channel, key);
        logger.info("read {}", read.getContentKey());
        ContentDaoUtil.compare(content, read, key.toString().getBytes());
    }

    @Test
    public void testHistorical() throws Exception {
        util.testWriteHistorical();
    }
}