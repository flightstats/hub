package com.flightstats.hub.cluster;

import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.Sleeper;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CuratorClusterTest {

    private static CuratorFramework curator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
    }

    @Test
    public void testPath() throws Exception {
        CuratorCluster cluster = new CuratorCluster(curator, "/test");
        Collection<String> servers = cluster.getServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());

        cluster.register();
        Sleeper.sleep(5000);

        servers = cluster.getServers();
        assertNotNull(servers);
        assertEquals(1, servers.size());

        cluster.delete();
        servers = cluster.getServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());
    }

}