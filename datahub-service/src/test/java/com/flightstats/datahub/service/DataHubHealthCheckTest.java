package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataHubHealthCheckTest {

    @Test
    public void testGet() throws Exception {
        ChannelDao channelDao = mock(ChannelDao.class);
        when(channelDao.isHealthy()).thenReturn(true);
        DataHubHealthCheck testClass = new DataHubHealthCheck(channelDao);
        assertTrue(testClass.isHealthy());
    }
}
