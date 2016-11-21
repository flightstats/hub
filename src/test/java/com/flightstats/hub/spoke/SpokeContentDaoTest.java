package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Injector;
import org.apache.curator.framework.CuratorFramework;
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
        CuratorFramework curator = injector.getInstance(CuratorFramework.class);
        CuratorCluster cluster = HubBindings.buildSpokeCuratorCluster(curator);
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
        HubProperties.setProperty("spoke.ttlMinutes", "240");
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
}