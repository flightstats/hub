package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.exception.AlreadyExistsException;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.rest.Linked;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ChannelsResourceTest {

    @Test
    public void testChannelCreation() throws Exception {
        String channelName = "UHF";
        String description = "We've got it all.";

        ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName, description);
        Date date = new Date() ;
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelName, description, date);
        Linked<ChannelConfiguration> expected = Linked.linked(channelConfiguration)
                                                      .withLink("self", "http://path/to/UHF")
                                                      .withLink("latest", "http://path/to/UHF/latest")
                                                      .build();
        UriInfo uriInfo = mock(UriInfo.class);
        ChannelDao dao = mock(ChannelDao.class);

        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to"));
        when(dao.channelExists(channelName)).thenReturn(false);
        when(dao.createChannel(channelName,  description)).thenReturn(channelConfiguration);

        ChannelsResource testClass = new ChannelsResource(uriInfo, dao);

        Linked<ChannelConfiguration> result = testClass.createChannel(channelCreationRequest);

        verify(dao).createChannel(channelName, description);

        assertEquals(expected, result);
    }

    @Test(expected = AlreadyExistsException.class)
    public void testCreate_channelAlreadyExists() throws Exception {
        String channelName = "UHF";
        String description = "We've got it all";
        ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName, description);

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.channelExists(channelName)).thenReturn(true);

        ChannelsResource testClass = new ChannelsResource(null, dao);
        testClass.createChannel(channelCreationRequest);
    }

}
