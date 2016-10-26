package com.flightstats.hub.util;

import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

import static com.flightstats.hub.util.ChannelNameUtils.getChannelName;
import static com.flightstats.hub.util.ChannelNameUtils.isValidChannelUrl;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelNameUtilsTest {

    @Test
    public void testGetChannelNameFromString() {
        assertEquals("foobar", getChannelName("http://location:8080/channel/foobar"));
        assertEquals("foobar", getChannelName("http://location:8080/channel/foobar/"));
        assertEquals("foobar", getChannelName("http://hub.svc.prod/channel/foobar/"));
    }

    @Test
    public void testGetChannelNameFromRequest() {
        ContainerRequestContext fooPath = mockRequest(singletonList("foo"), new ArrayList<>());
        assertEquals("foo", getChannelName(fooPath));

        ContainerRequestContext nullPath = mockRequest(new ArrayList<>(), new ArrayList<>());
        assertEquals("", getChannelName(nullPath));

        ContainerRequestContext fooHeader = mockRequest(new ArrayList<>(), singletonList("foo"));
        assertEquals("foo", getChannelName(fooHeader));

        ContainerRequestContext nullHeader = mockRequest(new ArrayList<>(), new ArrayList<>());
        assertEquals("", getChannelName(nullHeader));

        ContainerRequestContext firstHeader = mockRequest(new ArrayList<>(), asList("foo", "bar"));
        assertEquals("foo", getChannelName(firstHeader));

    }

    private ContainerRequestContext mockRequest(List<String> pathParameters, List<String> requestHeaders) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.put("channel", pathParameters);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put("channelName", requestHeaders);
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(parameters);
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    @Test
    public void testIsValidChannelUrl() {
        assertTrue(isValidChannelUrl("http://location:8080/channel/foobar"));
        assertFalse(isValidChannelUrl("http://location:8080/chann/foobar"));
        assertFalse(isValidChannelUrl("not a url"));
    }
}
