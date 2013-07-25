package com.flightstats.datahub.service;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LatestChannelItemResourceTest {

    @Test
    public void testGetLatest() throws Exception {
        String channelName = "fooChan";
        DataHubKey key = new DataHubKey(new Date(9998888777666L), (short) 0);
        DataHubKeyRenderer keyRenderer = new DataHubKeyRenderer();

        UriInfo uriInfo = mock(UriInfo.class);
        DataHubService dataHubService = mock(DataHubService.class);

        when(dataHubService.findLatestId(channelName)).thenReturn(Optional.of(key));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/channel/lolcats/latest"));

        LatestChannelItemResource testClass = new LatestChannelItemResource(uriInfo, dataHubService, keyRenderer);

        Response response = testClass.getLatest(channelName);
        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        List<Object> locations = response.getMetadata().get("Location");
        assertEquals(1, locations.size());
        assertEquals(URI.create("http://path/to/channel/lolcats/" + keyRenderer.keyToString(key)), locations.get(0));
    }

    @Test
    public void testGetLatest_channelEmpty() throws Exception {
        String channelName = "fooChan";

        DataHubService dataHubService = mock(DataHubService.class);

        when(dataHubService.findLatestId(channelName)).thenReturn(Optional.<DataHubKey>absent());

        LatestChannelItemResource testClass = new LatestChannelItemResource(null, dataHubService, null);

        try {
            testClass.getLatest(channelName);
            fail("Expected exception");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }
}
