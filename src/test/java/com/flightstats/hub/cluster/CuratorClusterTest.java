package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.test.IntegrationTestSetup;
import com.flightstats.hub.util.Sleeper;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class CuratorClusterTest {
    @Mock
    private SpokeProperties spokeProperties;
    @Mock
    private AppProperties appProperties;
    @Mock
    private LocalHostProperties localHostProperties;
    private CuratorFramework curator;

    @BeforeAll
    void setup() {
        curator = IntegrationTestSetup.run().getZookeeperClient();
    }

    @Test
    void testPath() throws Exception {
        log.info("starting testPath");

        final CuratorCluster cluster = new CuratorCluster(curator,
                "/CuratorClusterTest",
                false,
                true,
                new SpokeDecommissionCluster(curator, spokeProperties, localHostProperties),
                appProperties,
                spokeProperties,
                localHostProperties);

        when(appProperties.isReadOnly()).thenReturn(false);
        when(localHostProperties.getHost(false)).thenReturn("127.0.0.1:8080");

        Collection<String> servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());
        log.info("got expected 0");
        cluster.register();
        Sleeper.sleep(5000);
        log.info("slept 5000");
        servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(1, servers.size());
        log.info("got expected 1");
        cluster.delete();
        Sleeper.sleep(5000);
        servers = cluster.getAllServers();
        assertNotNull(servers);
        assertEquals(0, servers.size());
    }

}