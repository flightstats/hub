package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ValueInsertedResponse;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SingleChannelResourceTest {

    @Test
    public void testGetChannelMetadataForKnownChannel() throws Exception {
        String channelName = "UHF";

        ChannelDao dao = mock(ChannelDao.class);
        when(dao.channelExists(anyString())).thenReturn(true);

        SingleChannelResource testClass = new SingleChannelResource(dao, null);

        Map<String, String> result = testClass.getChannelMetadata(channelName);
        assertEquals(channelName, result.get("name"));
    }

    @Test
    public void testGetChannelMetadataForUnknownChannel() throws Exception {
        String channelName = "UHF";

        ChannelDao dao = mock(ChannelDao.class);
        when(dao.channelExists(anyString())).thenReturn(false);

        SingleChannelResource testClass = new SingleChannelResource(dao, null);
        try {
            testClass.getChannelMetadata(channelName);
            fail("Should have thrown a 404");
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testInsertValue() throws Exception {
        String channelName = "whizbang";
        byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};
        UUID uid = UUID.randomUUID();
        URI channelUri = URI.create("http://testification.com/channel/spoon");
        URI requestUri = URI.create("http://testification.com/channel/spoon");
        HalLink selfLink = new HalLink("self", URI.create(channelUri.toString() + "/" + uid));
        HalLink channelLink = new HalLink("channel", channelUri);

        ChannelDao dao = mock(ChannelDao.class);
        UriInfo urlInfo = mock(UriInfo.class);

        when(dao.channelExists(anyString())).thenReturn(true);
        when(dao.insert(channelName, data)).thenReturn(uid);
        when(urlInfo.getRequestUri()).thenReturn(requestUri);

        SingleChannelResource testClass = new SingleChannelResource(dao, urlInfo);
        Linked<ValueInsertedResponse> response = testClass.insertValue(channelName, data);

        assertThat(response.getLinks().getLinks(), hasItems(selfLink, channelLink));
        assertEquals(uid, response.getObject().getId());
    }

    @Test
    public void testInsertValue_unknownChannel() throws Exception {
        String channelName = "whizbang";
        byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};

        ChannelDao dao = mock(ChannelDao.class);
        UriInfo urlInfo = mock(UriInfo.class);

        when(dao.channelExists(anyString())).thenReturn(false);

        SingleChannelResource testClass = new SingleChannelResource(dao, urlInfo);
        try {
            testClass.insertValue(channelName, data);
            fail("Should have thrown an exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }
}
