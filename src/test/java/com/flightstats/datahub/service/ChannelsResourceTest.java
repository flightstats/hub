package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.exception.AlreadyExistsException;
import com.flightstats.datahub.model.ChannelCreationRequest;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelsResourceTest {

    @Test
    public void testChannelCreation() throws Exception {
        String channelName = "UHF";
        String description = "We've got it all.";

        ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName, description);
        UriInfo uriInfo = mock(UriInfo.class);

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.channelExists(channelName)).thenReturn(false);

        ChannelsResource testClass = new ChannelsResource(uriInfo, dao);
        testClass.createChannel(channelCreationRequest);

        verify(dao).createChannel(channelName, description);
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
