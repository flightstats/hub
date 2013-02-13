package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.rest.Linked;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
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

    @Test(expected = WebApplicationException.class)
    public void testGetChannelMetadataForUnknownChannel() throws Exception {
        String channelName = "UHF";

        ChannelDao dao = mock(ChannelDao.class);
        when(dao.channelExists(anyString())).thenReturn(false);

        ChannelResource testClass = new ChannelResource(null, dao);
        Map<String, String> result = testClass.getChannelMetadata(channelName);
        assertEquals(channelName, result.get("name"));
    }
}
