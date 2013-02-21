package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.rest.HalLink;
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
        String contentType = "text/plain";
        UUID uid = UUID.randomUUID();
        URI channelUri = URI.create("http://testification.com/channel/spoon");
        URI requestUri = URI.create("http://testification.com/channel/spoon");
        HalLink selfLink = new HalLink("self", URI.create(channelUri.toString() + "/" + uid));
        HalLink channelLink = new HalLink("channel", channelUri);
        Date date = new Date(123456L);
        ValueInsertionResult expectedResponse = new ValueInsertionResult(uid, date);

        ChannelDao dao = mock(ChannelDao.class);
        UriInfo urlInfo = mock(UriInfo.class);

        when(dao.channelExists(anyString())).thenReturn(true);
        when(dao.insert(channelName, contentType, data)).thenReturn(new ValueInsertionResult(uid, date));
        when(urlInfo.getRequestUri()).thenReturn(requestUri);

        SingleChannelResource testClass = new SingleChannelResource(dao, urlInfo);
        Linked<ValueInsertionResult> response = testClass.insertValue(contentType, channelName, data);

        assertThat(response.getLinks().getLinks(), hasItems(selfLink, channelLink));
        ValueInsertionResult insertionResult = response.getObject();

        assertEquals(expectedResponse, insertionResult);
    }

    @Test
    public void testInsertValue_unknownChannel() throws Exception {
        String channelName = "whizbang";
        byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};
        String contentType = "text/plain";

        ChannelDao dao = mock(ChannelDao.class);
        UriInfo urlInfo = mock(UriInfo.class);

        when(dao.channelExists(anyString())).thenReturn(false);

        SingleChannelResource testClass = new SingleChannelResource(dao, urlInfo);
        try {
            testClass.insertValue(contentType, channelName, data);
            fail("Should have thrown an exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }
}
