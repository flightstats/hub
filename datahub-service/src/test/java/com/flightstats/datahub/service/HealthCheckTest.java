package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HealthCheckTest {

    @Test
    public void testGet() throws Exception {
        ChannelDao channelDao = mock(ChannelDao.class);
        when(channelDao.countChannels()).thenReturn(5);
        HealthCheck testClass = new HealthCheck(channelDao);
        String result = testClass.check();
        assertEquals("OK (5 channels)", result);
    }
}
