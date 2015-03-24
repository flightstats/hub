package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpokeContentDaoTest {

    private final static Logger logger = LoggerFactory.getLogger(SpokeContentDaoTest.class);
    private static ContentDaoUtil util;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = Integration.startAwsHub();
        util = new ContentDaoUtil(injector.getInstance(SpokeContentDao.class));
        CuratorSpokeCluster cluster = injector.getInstance(CuratorSpokeCluster.class);
        for (int i = 0; i < 10; i++) {
            if (cluster.getServers().size() == 0) {
                logger.info("no servers yet...");
                Sleeper.sleep(500);
            } else {
                logger.info("servers {}", cluster.getServers());
                return;
            }
        }
        logger.info("no servers found");
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