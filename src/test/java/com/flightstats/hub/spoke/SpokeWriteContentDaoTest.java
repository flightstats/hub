package com.flightstats.hub.spoke;

import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.Integration;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;

@Slf4j
public class SpokeWriteContentDaoTest {

    private static ContentDaoUtil util;

    @BeforeClass
    public static void setUpClass() throws Exception {
        util = new ContentDaoUtil(Integration.startAwsHub().getInstance(SpokeWriteContentDao.class));
    }

    @Test
    public void testWriteRead() throws Exception {
        Content content = ContentDaoUtil.createContent();
        util.testWriteRead(content);
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
        PropertiesLoader.getInstance().setProperty("spoke.write.ttlMinutes", "240");
        util.testDirectionQueryTTL();
    }

    @Test
    public void testEarliest() throws Exception {
        util.testEarliest();
    }

    @Test
    public void testBulkWrite() throws Exception {
        util.testBulkWrite();
    }

    @Test
    public void testEmptyQuery() throws Exception {
        util.testEmptyQuery();

    }

    @Test
    public void testEmptyQueryReplicated() throws Exception {
        util.testEmptyQuery();

    }

    @Test
    public void testPreviousFromBulk_Issue753() throws Exception {
        util.testPreviousFromBulk_Issue753();
    }
}