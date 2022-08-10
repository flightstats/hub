package com.flightstats.hub.util;

import org.junit.jupiter.api.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import java.util.Collections;

import static com.flightstats.hub.util.RequestUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestUtilsTest {

    @Test
    void testGetChannelNameFromString() {
        assertEquals("foobar", getChannelName("http://location:8080/channel/foobar"));
        assertEquals("foobar", getChannelName("http://location:8080/channel/foobar/"));
        assertEquals("foobar", getChannelName("http://hub.prod/channel/foobar/"));
    }

    @Test
    void testGetTagFromString() {
        assertEquals("foobar", getTag("http://location:8080/tag/foobar"));
        assertEquals("foobar", getTag("http://location:8080/tag/foobar/"));
        assertEquals("foobar", getTag("http://hub.prod/tag/foobar/"));
    }

    @Test
    void testIsValidChannelUrl() {
        assertTrue(isValidChannelUrl("http://location:8080/channel/foobar"));
        assertFalse(isValidChannelUrl("http://location:8080/chann/foobar"));
        assertFalse(isValidChannelUrl("not a url"));
    }

    @Test
    void testGetHost() {
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
