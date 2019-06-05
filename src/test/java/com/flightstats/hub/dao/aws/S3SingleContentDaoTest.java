package com.flightstats.hub.dao.aws;

import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.IntegrationTestSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class S3SingleContentDaoTest {

    private ContentDaoUtil util;
    private S3SingleContentDao s3SingleContentDao;

    @BeforeAll
    void setUpClass() {
        s3SingleContentDao = IntegrationTestSetup.run().getInstance(S3SingleContentDao.class);
    }

    @BeforeEach
    void setup() {
        util = new ContentDaoUtil(s3SingleContentDao);
    }

    @Test
    void testWriteRead() throws Exception {
        util.testWriteRead(ContentDaoUtil.createContent());
    }

    @Test
    void testQueryRangeDay() {
        util.testQueryRangeDay();
    }

    @Test
    void testQueryRangeHour() {
        util.testQueryRangeHour();
    }

    @Test
    void testQueryRangeMinute() {
        util.testQueryRangeMinute();
    }

    @Test
    void testQuery15Minutes() {
        util.testQuery15Minutes();
    }

    @Test
    void testDirectionQuery() {
        util.testDirectionQuery();
    }

    @Test
    void testDelete() {
        util.testDeleteMaxItems();
    }

    @Test
    void testPreviousFromBulk_Issue753() {
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