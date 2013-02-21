package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.google.common.base.Optional;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LatestChannelItemResourceTest {

    @Test
    public void testGetLatest() throws Exception {
        String channelName = "fooChan";
        UUID latest = UUID.randomUUID();

        UriInfo uriInfo = mock(UriInfo.class);
        ChannelDao channelDao = mock(ChannelDao.class);

        when(channelDao.findLatestId(channelName)).thenReturn(Optional.of(latest));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/channel/lolcats/latest"));

        LatestChannelItemResource testClass = new LatestChannelItemResource(uriInfo, channelDao);

        Response response = testClass.getLatest(channelName);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        List<Object> locations = response.getMetadata().get("Location");
        assertEquals(1, locations.size());
        assertEquals(URI.create("http://path/to/channel/lolcats/" + latest.toString()), locations.get(0));
    }

    @Test
    public void testGetLatest_channelEmpty() throws Exception {
        fail("Build me.");
    }
}
