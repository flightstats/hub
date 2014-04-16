package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HubHealthCheckTest {

    @Test
    public void testGet() throws Exception {
        ChannelService channelService = mock(ChannelService.class);
        when(channelService.isHealthy()).thenReturn(true);
        HubHealthCheck testClass = new HubHealthCheck(channelService);
        assertTrue(testClass.isHealthy());
    }
}
