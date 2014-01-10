package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ZooKeeperState;
import com.flightstats.datahub.dao.ChannelService;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataHubHealthCheckTest {

    @Test
    public void testGet() throws Exception {
        ChannelService channelService = mock(ChannelService.class);
        when(channelService.isHealthy()).thenReturn(true);
        ZooKeeperState zooKeeperState = mock(ZooKeeperState.class);
        when(zooKeeperState.isHealthy()).thenReturn(true);
        DataHubHealthCheck testClass = new DataHubHealthCheck(channelService, zooKeeperState);
        assertTrue(testClass.isHealthy());
    }
}
