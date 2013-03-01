package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SingleChannelResourceTest {

    private String channelName;
    private String contentType;
    private URI channelUri;
    private URI requestUri;
    private UriInfo urlInfo;
    private ChannelDao dao;

    @Before
    public void setup() {
        channelName = "UHF";
        contentType = "text/plain";
        channelUri = URI.create("http://testification.com/channel/spoon");
        requestUri = URI.create("http://testification.com/channel/spoon");
        urlInfo = mock(UriInfo.class);
        dao = mock(ChannelDao.class);

        when(urlInfo.getRequestUri()).thenReturn(requestUri);
        when(dao.channelExists(channelName)).thenReturn(true);
    }

    @Test
    public void testGetChannelMetadataForKnownChannel() throws Exception {
        Date creationDate = new Date(12345L);
        ChannelConfiguration expectedConfig = new ChannelConfiguration(channelName, creationDate, null);

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
        when(dao.channelExists("unknownChannel")).thenReturn(false);

        SingleChannelResource testClass = new SingleChannelResource(dao, null, new DataHubKeyRenderer());
        try {
            testClass.getChannelMetadata("unknownChannel");
            fail("Should have thrown a 404");
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testInsertValue() throws Exception {
        byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};
        Date date = new Date(123456L);
        DataHubKey key = new DataHubKey(date, (short) 5);

        HalLink selfLink = new HalLink("self", URI.create(channelUri.toString() + "/0000000007H40005"));
        HalLink channelLink = new HalLink("channel", channelUri);
        ValueInsertionResult expectedResponse = new ValueInsertionResult(key);

        when(dao.insert(channelName, contentType, data)).thenReturn(new ValueInsertionResult(key));

        SingleChannelResource testClass = new SingleChannelResource(dao, urlInfo, new DataHubKeyRenderer());
        Response response = testClass.insertValue(contentType, channelName, data);
        Linked<ValueInsertionResult> result = (Linked<ValueInsertionResult>) response.getEntity();

        assertThat(result.getLinks().getLinks(), hasItems(selfLink, channelLink));
        ValueInsertionResult insertionResult = result.getObject();

        assertEquals(expectedResponse, insertionResult);
        assertEquals(URI.create("http://testification.com/channel/spoon/0000000007H40005"), response.getMetadata().getFirst("Location"));
    }

    @Test
    public void testInsertValue_unknownChannel() throws Exception {
        byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};

        ChannelDao dao = mock(ChannelDao.class);

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
