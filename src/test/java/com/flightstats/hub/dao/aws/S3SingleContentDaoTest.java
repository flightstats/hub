package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Slf4j
public class S3SingleContentDaoTest {

    private static ContentDaoUtil util;
    private static S3SingleContentDao s3SingleContentDao;

    @BeforeClass
    public static void setUpClass() throws Exception {
        PropertiesLoader.getInstance().load("useDefault");
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
    public void testWriteReadOld() {
        String channel = "testWriteReadOld";
        Content content = ContentDaoUtil.createContent();
        ContentKey key = s3SingleContentDao.insertOld(channel, content);
        log.info("key {}", key);
        assertEquals(content.getContentKey().get(), key);
        Content read = s3SingleContentDao.get(channel, key);
        log.info("read {}", read.getContentKey());
        ContentDaoUtil.compare(content, read, key.toString().getBytes());
    }

    @Test
    public void testHistorical() throws Exception {
        util.testWriteHistorical();
    }
}