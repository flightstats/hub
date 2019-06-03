package com.flightstats.hub.spoke;

import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.IntegrationTestSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpokeWriteContentDaoTest {

    private ContentDaoUtil util;

    @BeforeAll
    void setUpClass() {
        util = new ContentDaoUtil(IntegrationTestSetup.run().getInstance(SpokeWriteContentDao.class));
    }

    @Test
    void testWriteRead() throws Exception {
        Content content = ContentDaoUtil.createContent();
        util.testWriteRead(content);
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
        PropertiesLoader.getInstance().setProperty("spoke.write.ttlMinutes", "240");
        util.testDirectionQueryTTL();
    }

    @Test
    void testEarliest() {
        util.testEarliest();
    }

    @Test
    void testBulkWrite() {
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
    void testPreviousFromBulk_Issue753() {
        util.testPreviousFromBulk_Issue753();
    }
}