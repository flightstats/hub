package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.SpokeDecommissionCluster;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Injector;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SpokeWriteContentDaoTest {

    private final static Logger logger = LoggerFactory.getLogger(SpokeWriteContentDaoTest.class);
    private static ContentDaoUtil util;

    @BeforeAll
    static void setUpClass() throws Exception {
        Injector injector = Integration.startAwsHub();
        util = new ContentDaoUtil(injector.getInstance(SpokeWriteContentDao.class));
        CuratorFramework curator = injector.getInstance(CuratorFramework.class);
        Cluster cluster = HubBindings.buildSpokeCluster(curator, new SpokeDecommissionCluster(curator));
        for (int i = 0; i < 10; i++) {
            if (cluster.getAllServers().size() == 0) {
                logger.info("no servers yet...");
                Sleeper.sleep(500);
            } else {
                logger.info("servers {}", cluster.getAllServers());
                return;
            }
        }
        logger.info("no servers found");
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
        HubProperties.setProperty("spoke.write.ttlMinutes", "240");
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