package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.IntegrationTestSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class S3SingleContentDaoTest {

    private static ContentDaoUtil util;
    private static S3SingleContentDao s3SingleContentDao;

    @BeforeAll
    static void setUpClass() {
        PropertiesLoader.getInstance().load("useDefault");
        s3SingleContentDao = IntegrationTestSetup.run().getInstance(S3SingleContentDao.class);
        util = new ContentDaoUtil(s3SingleContentDao);
    }

    @Test
    void testWriteRead() throws Exception {
        util.testWriteRead(ContentDaoUtil.createContent());
    }

    @Test
    void testQueryRangeDay() throws Exception {
        util.testQueryRangeDay();
    }

    @Test
    void testQueryRangeHour() throws Exception {
        util.testQueryRangeHour();
    }

    @Test
    void testQueryRangeMinute() throws Exception {
        util.testQueryRangeMinute();
    }

    @Test
    void testQuery15Minutes() throws Exception {
        util.testQuery15Minutes();
    }

    @Test
    void testDirectionQuery() throws Exception {
        util.testDirectionQuery();
    }

    @Test
    void testDelete() throws Exception {
        util.testDeleteMaxItems();
    }

    @Test
    void testPreviousFromBulk_Issue753() throws Exception {
        util.testPreviousFromBulk_Issue753();
    }

    @Test
    void testWriteReadOld() {
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
    void testHistorical() throws Exception {
        util.testWriteHistorical();
    }
}