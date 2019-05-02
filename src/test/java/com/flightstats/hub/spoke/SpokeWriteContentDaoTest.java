package com.flightstats.hub.spoke;

import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.SpokeDecommissionCluster;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.config.binding.HubBindings;
import com.flightstats.hub.dao.ContentDaoUtil;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;

@Slf4j
public class SpokeWriteContentDaoTest {

    private static ContentDaoUtil util;

    @BeforeClass
    public static void setUpClass() throws Exception {

        final Injector injector = Integration.startAwsHub();
        util = new ContentDaoUtil(injector.getInstance(SpokeWriteContentDao.class));
        final CuratorFramework curator = injector.getInstance(CuratorFramework.class);

        final SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());
        Cluster cluster = HubBindings.buildSpokeCluster(curator,
                new SpokeDecommissionCluster(curator, spokeProperties),
                new AppProperties(PropertiesLoader.getInstance()), spokeProperties);

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