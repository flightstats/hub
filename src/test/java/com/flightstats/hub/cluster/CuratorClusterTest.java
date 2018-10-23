package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.test.TestMain;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Injector;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CuratorClusterTest {

    private final static Logger logger = LoggerFactory.getLogger(CuratorClusterTest.class);

    @Test
    public void testPath() throws Exception {
        Injector injector = TestMain.start();
        HubProperties hubProperties = injector.getInstance(HubProperties.class);
        CuratorFramework curatorFramework = injector.getInstance(CuratorFramework.class);
        SpokeDecommissionCluster spokeDecommissionCluster = injector.getInstance(SpokeDecommissionCluster.class);
        CuratorCluster cluster = new CuratorCluster(curatorFramework, "/SpokeCluster", false, true, spokeDecommissionCluster, hubProperties);

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
