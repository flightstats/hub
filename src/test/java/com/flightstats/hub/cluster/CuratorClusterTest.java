package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.Sleeper;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CuratorClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(CuratorClusterTest.class);

    private static CuratorFramework curator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Test
    public void testPath() throws Exception {
        logger.info("starting testPath");
        HubUtils hubUtils = new HubUtils(HubBindings.buildJerseyClientNoRedirects(), HubBindings.buildJerseyClient());
        CuratorCluster cluster = new CuratorCluster(curator, "/SpokeCluster", false, new SpokeDecommissionCluster(curator, hubUtils));

        Collection<String> servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());
        logger.info("got expected 0");
        cluster.register();
        Sleeper.sleep(5000);
        logger.info("slept 5000");
        servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(1, servers.size());
        logger.info("got expected 1");
        cluster.delete();
        Sleeper.sleep(5000);
        servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());
    }

}