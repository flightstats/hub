package com.flightstats.hub.util;

import org.junit.jupiter.api.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import static com.flightstats.hub.util.RequestUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestUtilsTest {

    @Test
    public void testGetChannelNameFromString() {
        assertEquals("foobar", getChannelName("http://location:8080/channel/foobar"));
        assertEquals("foobar", getChannelName("http://location:8080/channel/foobar/"));
        assertEquals("foobar", getChannelName("http://hub.prod/channel/foobar/"));
    }

    @Test
    public void testGetTagFromString() {
        assertEquals("foobar", getTag("http://location:8080/tag/foobar"));
        assertEquals("foobar", getTag("http://location:8080/tag/foobar/"));
        assertEquals("foobar", getTag("http://hub.prod/tag/foobar/"));
    }

    @Test
    public void testGetChannelNameFromRequest() {
        MultivaluedMap<String, String> emptyMap = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        ContainerRequestContext noChannels = mockRequest(emptyMap, emptyMap);
        assertEquals("", getChannelName(noChannels));

        parameters.put("channel", singletonList("foo"));
        ContainerRequestContext fooPath = mockRequest(parameters, emptyMap);
        assertEquals("foo", getChannelName(fooPath));

        headers.put("channelName", singletonList("bar"));
        ContainerRequestContext fooHeader = mockRequest(emptyMap, headers);
        assertEquals("bar", getChannelName(fooHeader));

        headers.put("channelName", asList("baz", "zab"));
        ContainerRequestContext firstHeader = mockRequest(emptyMap, headers);
        assertEquals("baz", getChannelName(firstHeader));

        ContainerRequestContext headerAndPath = mockRequest(parameters, headers);
        assertEquals("foo", getChannelName(headerAndPath));
    }

    @Test
    public void testGetTagFromRequest() {
        MultivaluedMap<String, String> emptyMap = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();

        ContainerRequestContext noTags = mockRequest(emptyMap, emptyMap);
        assertEquals("", getTag(noTags));

        parameters.put("tag", singletonList("foo"));
        ContainerRequestContext fooTag = mockRequest(parameters, emptyMap);
        assertEquals("foo", getTag(fooTag));

        parameters.put("tag", asList("bar", "baz"));
        ContainerRequestContext firstTag = mockRequest(parameters, emptyMap);
        assertEquals("bar", getTag(firstTag));
    }

    private ContainerRequestContext mockRequest(MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> headers) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParameters);
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    @Test
    public void testIsValidChannelUrl() {
        assertTrue(isValidChannelUrl("http://location:8080/channel/foobar"));
        assertFalse(isValidChannelUrl("http://location:8080/chann/foobar"));
        assertFalse(isValidChannelUrl("not a url"));
    }

    @Test
    public void testGetHost() {
        String host1 = "http://stuff.com";
        String host2 = "http://stuff.com:99";

        assertEquals(host1, getHost(host1));
        assertEquals(host2, getHost(host2));
        assertEquals(host1, getHost(host1 + "/"));
        assertEquals(host2, getHost(host2 + "/"));
        assertEquals(host1, getHost(host1 + "/abc/def"));
        assertEquals(host2, getHost(host2 + "/abc/def"));

    }
}
