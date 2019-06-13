package com.flightstats.hub.cluster;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.test.IntegrationTestSetup;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpokeDecommissionClusterTest {
    @Mock
    private LocalHostProperties localHostProperties;
    @Mock
    private SpokeProperties spokeProperties;
    private SpokeDecommissionCluster cluster;
    private CuratorFramework curator;

    @BeforeAll
    void setup() {
         curator = IntegrationTestSetup.run().getZookeeperClient();
    }

    @Test
    void testDecommission() throws Exception {
        when(localHostProperties.getHost(false)).thenReturn("127.0.0.1:8080");
        cluster = new SpokeDecommissionCluster(curator, spokeProperties, localHostProperties);
        cluster.decommission();
        assertTrue(cluster.withinSpokeExists());
        assertFalse(cluster.doNotRestartExists());

        assertEquals(spokeProperties.getTtlMinutes(SpokeStore.WRITE), cluster.getDoNotRestartMinutes(), 1);

        cluster.doNotRestart();
        assertFalse(cluster.withinSpokeExists());
        assertTrue(cluster.doNotRestartExists());
    }

    @AfterEach
    void afterTest() {
        cluster.doNotRestart();
    }

}