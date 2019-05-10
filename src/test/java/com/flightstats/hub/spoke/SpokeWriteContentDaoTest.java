package com.flightstats.hub.spoke;

import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.Integration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
class SpokeWriteContentDaoTest {

    private static ContentDaoUtil util;

    @BeforeAll
    static void setUpClass() throws Exception {
        util = new ContentDaoUtil(Integration.startAwsHub().getInstance(SpokeWriteContentDao.class));
    }

    @Test
    void testWriteRead() throws Exception {
        Content content = ContentDaoUtil.createContent();
        util.testWriteRead(content);
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
        PropertiesLoader.getInstance().setProperty("spoke.write.ttlMinutes", "240");
        util.testDirectionQueryTTL();
    }

    @Test
    void testEarliest() throws Exception {
        util.testEarliest();
    }

    @Test
    void testBulkWrite() throws Exception {
        util.testBulkWrite();
    }

    @Test
    void testEmptyQuery() {
        util.testEmptyQuery();

    }

    @Test
    void testEmptyQueryReplicated() {
        util.testEmptyQuery();

    }

    @Test
    void testPreviousFromBulk_Issue753() throws Exception {
        util.testPreviousFromBulk_Issue753();
    }
}