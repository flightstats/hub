package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.IntegrationTestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpokeWriteContentDaoTest {

    private ContentDaoUtil util;
    private SpokeWriteContentDao spokeWriteContentDao;

    @BeforeAll
    void setUpClass() {
        spokeWriteContentDao = IntegrationTestSetup.run().getInstance(SpokeWriteContentDao.class);
    }

    @BeforeEach
    void setup(){
        util = new ContentDaoUtil(spokeWriteContentDao);
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