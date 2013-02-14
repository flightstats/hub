package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.rest.Linked;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.*;

public class ChannelResourceTest {

    @Test
    public void testChannelCreation() throws Exception {
        String channelName = "UHF";

        ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName);
        Date date = new Date();
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelName, date);
        Linked<ChannelConfiguration> expected = Linked.linked(channelConfiguration)
                                                      .withLink("self", "http://path/to/UHF")
                                                      .build();
        UriInfo uriInfo = mock(UriInfo.class);
        ChannelDao dao = mock(ChannelDao.class);

        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to"));
        when(dao.channelExists(channelName)).thenReturn(false);
        when(dao.createChannel(channelName)).thenReturn(channelConfiguration);

        ChannelResource testClass = new ChannelResource(uriInfo, dao);

        Linked<ChannelConfiguration> result = testClass.createChannel(channelCreationRequest);

        verify(dao).createChannel(channelName);

        assertEquals(expected, result);
    }

    @Test
    public void testGetChannelMetadataForKnownChannel() throws Exception {
        String channelName = "UHF";

        ChannelDao dao = mock(ChannelDao.class);
        when(dao.channelExists(anyString())).thenReturn(true);

        ChannelResource testClass = new ChannelResource(null, dao);

        Map<String, String> result = testClass.getChannelMetadata(channelName);
        assertEquals(channelName, result.get("name"));

    }

    @Test
    public void testGetChannelMetadataForUnknownChannel() throws Exception {
        String channelName = "UHF";

        ChannelDao dao = mock(ChannelDao.class);
        when(dao.channelExists(anyString())).thenReturn(false);

        ChannelResource testClass = new ChannelResource(null, dao);
        try {
            testClass.getChannelMetadata(channelName);
            fail("Should have thrown a 404");
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGetValue() throws Exception {

        String channelName = "canal4";
        UUID uid = UUID.randomUUID();
        byte[] expected = new byte[]{55, 66, 77, 88};

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, uid)).thenReturn(expected);

        ChannelResource testClass = new ChannelResource(null, dao);
        byte[] result = testClass.getValue(channelName, uid);

        assertEquals(expected, result);
    }

    @Test
    public void testGetValueNotFound() throws Exception {

        String channelName = "canal4";
        UUID uid = UUID.randomUUID();

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, uid)).thenReturn(null);

        ChannelResource testClass = new ChannelResource(null, dao);
        try {
            testClass.getValue(channelName, uid);
            fail("Should have thrown exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }

    }
}
