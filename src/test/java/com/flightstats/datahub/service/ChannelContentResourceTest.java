package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelContentResourceTest {

    @Test
    public void testGetValue() throws Exception {

        String channelName = "canal4";
        UUID uid = UUID.randomUUID();
        byte[] expected = new byte[]{55, 66, 77, 88};

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, uid)).thenReturn(expected);

        ChannelContentResource testClass = new ChannelContentResource(dao);
        byte[] result = testClass.getValue(channelName, uid);

        assertEquals(expected, result);
    }

    @Test
    public void testGetValueNotFound() throws Exception {

        String channelName = "canal4";
        UUID uid = UUID.randomUUID();

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, uid)).thenReturn(null);

        ChannelContentResource testClass = new ChannelContentResource(dao);
        try {
            testClass.getValue(channelName, uid);
            fail("Should have thrown exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }

    }
}
