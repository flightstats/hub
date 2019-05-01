package com.flightstats.hub.spoke;

import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.SpokeDecommissionCluster;
import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.PropertyLoader;
import com.flightstats.hub.config.SpokeProperty;
import com.flightstats.hub.config.binding.HubBindings;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
class SpokeWriteContentDaoTest {

    private static ContentDaoUtil util;

    @BeforeAll
    static void setUpClass() throws Exception {
        final Injector injector = Integration.startAwsHub();
        util = new ContentDaoUtil(injector.getInstance(SpokeWriteContentDao.class));
        final CuratorFramework curator = injector.getInstance(CuratorFramework.class);

        final SpokeProperty spokeProperty = new SpokeProperty(PropertyLoader.getInstance());
        Cluster cluster = HubBindings.buildSpokeCluster(curator,
                new SpokeDecommissionCluster(curator, spokeProperty),
                new AppProperty(PropertyLoader.getInstance()), spokeProperty);

        for (int i = 0; i < 10; i++) {
            if (cluster.getAllServers().size() == 0) {
                log.info("no servers yet...");
                Sleeper.sleep(500);
            } else {
                log.info("servers {}", cluster.getAllServers());
                return;
            }
        }
        log.info("no servers found");
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
        PropertyLoader.getInstance().setProperty("spoke.write.ttlMinutes", "240");
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