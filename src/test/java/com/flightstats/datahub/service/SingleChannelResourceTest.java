package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;

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
        Date creationDate = new Date(12345L);
        ChannelConfiguration expectedConfig = new ChannelConfiguration(channelName, creationDate, null);

        ChannelDao dao = mock(ChannelDao.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(dao.channelExists(anyString())).thenReturn(true);
        when(dao.getChannelConfiguration(channelName)).thenReturn(expectedConfig);
        URI channelUri = URI.create("http://fubar.com/channel/gomez");
        when(uriInfo.getRequestUri()).thenReturn(channelUri);

        SingleChannelResource testClass = new SingleChannelResource(dao, uriInfo, new DataHubKeyRenderer());

        Linked<ChannelConfiguration> result = testClass.getChannelMetadata(channelName);
        assertEquals(expectedConfig, result.getObject());
        HalLink selfLink = result.getLinks().getLinks().get(0);
        HalLink latestLink = result.getLinks().getLinks().get(1);
        assertEquals(new HalLink("self", channelUri), selfLink);
        assertEquals(new HalLink("latest", URI.create(channelUri.toString() + "/latest")), latestLink);
    }

    @Test
    public void testGetChannelMetadataForUnknownChannel() throws Exception {
        String channelName = "UHF";

        ChannelDao dao = mock(ChannelDao.class);
        when(dao.channelExists(anyString())).thenReturn(false);

        SingleChannelResource testClass = new SingleChannelResource(dao, null, new DataHubKeyRenderer());
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
        Date date = new Date(123456L);
        DataHubKey key = new DataHubKey(date, (short) 5);
        URI channelUri = URI.create("http://testification.com/channel/spoon");
        URI requestUri = URI.create("http://testification.com/channel/spoon");
        HalLink selfLink = new HalLink("self", URI.create(channelUri.toString() + "/0000000007H40005"));
        HalLink channelLink = new HalLink("channel", channelUri);
        ValueInsertionResult expectedResponse = new ValueInsertionResult(key);

        ChannelDao dao = mock(ChannelDao.class);
        UriInfo urlInfo = mock(UriInfo.class);

        when(dao.channelExists(anyString())).thenReturn(true);
        when(dao.insert(channelName, contentType, data)).thenReturn(new ValueInsertionResult(key));
        when(urlInfo.getRequestUri()).thenReturn(requestUri);

        SingleChannelResource testClass = new SingleChannelResource(dao, urlInfo, new DataHubKeyRenderer());
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

        SingleChannelResource testClass = new SingleChannelResource(dao, urlInfo, new DataHubKeyRenderer());
        try {
            testClass.insertValue(contentType, channelName, data);
            fail("Should have thrown an exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }
}
