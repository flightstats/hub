package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.test.Integration;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Test;

public class SpokeContentDaoTest {

    private static ContentDaoUtil util;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = Integration.startAwsHub();
        util = new ContentDaoUtil(injector.getInstance(SpokeContentDao.class));
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
}