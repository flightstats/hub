package com.flightstats.hub.spoke;

import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CuratorSpokeClusterTest {

    private static CuratorFramework curator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Test
    public void testPath() throws Exception {
        CuratorSpokeCluster cluster = new CuratorSpokeCluster(curator, 8080);
        List<String> servers = cluster.getServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());

        cluster.register();
        Sleeper.sleep(1000);

        servers = cluster.getServers();
        assertNotNull(servers);
        assertEquals(1, servers.size());
    }

}