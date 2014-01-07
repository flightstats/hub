package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.dao.TimeIndex;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.TimeSeriesContentKey;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ChannelTimeResourceTest {

    private ChannelTimeResource resource;
    private ChannelService channelService;
    private String channelName;
    public static final String BASE_URL = "http://testy:1234/";

    @Before
    public void setUp() throws Exception {
        channelName = "keyTime";
        UriInfo uriInfo = mock(UriInfo.class);
        channelService = mock(ChannelService.class);
        ChannelHypermediaLinkBuilder linkBuilder = new ChannelHypermediaLinkBuilder();
        resource = new ChannelTimeResource(uriInfo, channelService, linkBuilder);
        when(uriInfo.getBaseUri()).thenReturn(new URI(BASE_URL));
        when(uriInfo.getRequestUri()).thenReturn(new URI(BASE_URL + "channel/" + channelName + "/ids/2013-12-27T11:50-0800"));
    }

    @Test
    public void testNormal() throws Exception {
        DateTime dateTime = new DateTime();
        String hashStamp = TimeIndex.getHash(dateTime);
        DateTime expectedDate = TimeIndex.parseHash(hashStamp);
        ContentKey second = new TimeSeriesContentKey();
        ContentKey first = new TimeSeriesContentKey();
        Iterable<ContentKey> contentKeys = Lists.newArrayList(first, second);
        when(channelService.getKeys(channelName, expectedDate)).thenReturn(contentKeys);
        Response response = resource.getValue(channelName, hashStamp);
        String entity = response.getEntity().toString();
        assertEquals("{\"_links\":{\"self\":{\"href\":\"" + BASE_URL + "channel/" + channelName + "/ids/2013-12-27T11:50-0800\"}," +
                "\"uris\":[" +
                "\"" + BASE_URL + "channel/" + channelName + "/" + first.keyToString() + "\"," +
                "\"" + BASE_URL + "channel/" + channelName + "/" + second.keyToString() + "\"" +
                "]}}", entity);

    }

    @Test(expected = InvalidRequestException.class)
    public void testWrongDateFormat() throws Exception {
        resource.getValue(channelName, "2013-12-27");
    }

    @Test
    public void testEmptyResults() throws Exception {
        DateTime dateTime = new DateTime();
        String hashStamp = TimeIndex.getHash(dateTime);
        DateTime expectedDate = TimeIndex.parseHash(hashStamp);
        Iterable<ContentKey> contentKeys = new ArrayList<>();
        when(channelService.getKeys(channelName, expectedDate)).thenReturn(contentKeys);
        Response response = resource.getValue(channelName, hashStamp);
        String entity = response.getEntity().toString();
        assertEquals("{\"_links\":{\"self\":{\"href\":\"" + BASE_URL + "channel/" + channelName + "/ids/2013-12-27T11:50-0800\"}," +
                "\"uris\":[]}}", entity);
    }
}
