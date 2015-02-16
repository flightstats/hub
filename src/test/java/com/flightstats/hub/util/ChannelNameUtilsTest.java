package com.flightstats.hub.util;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class ChannelNameUtilsTest {

    private ChannelNameUtils utils;

    @Before
    public void setUp() throws Exception {
        utils = new ChannelNameUtils();
    }

    @Test
    public void testExtractChannelName() throws Exception {
        assertEquals("foobar", utils.extractFromWS(URI.create("/channel/foobar/ws")));
    }

    @Test
    public void testWhenChannelDoesntMatch() throws Exception {
        String uriString = "/shouldnt/find/me";
        String result = utils.extractFromWS(URI.create(uriString));
        assertEquals(uriString, result);
    }

    @Test
    public void testExtractChannelUri() throws Exception {
        assertEquals("foobar", ChannelNameUtils.extractFromChannelUrl(URI.create("http://location:8080/channel/foobar/")));
        assertEquals("foobar", ChannelNameUtils.extractFromChannelUrl(URI.create("http://location:8080/channel/foobar")));
        assertEquals("foobar", ChannelNameUtils.extractFromChannelUrl(URI.create("http://hub.svc.prod/channel/foobar")));
    }

    @Test
    public void testExtractChannelString() throws Exception {
        assertEquals("foobar", ChannelNameUtils.extractFromChannelUrl("http://location:8080/channel/foobar"));
        assertEquals("foobar", ChannelNameUtils.extractFromChannelUrl("http://location:8080/channel/foobar/"));
        assertEquals("foobar", ChannelNameUtils.extractFromChannelUrl("http://hub.svc.prod/channel/foobar/"));
    }

    @Test
    public void testValidChannelUrl() throws Exception {
        assertTrue(ChannelNameUtils.isValidChannelUrl("http://location:8080/channel/foobar"));
    }

    @Test
    public void testInvalidChannelUrl() throws Exception {
        assertFalse(ChannelNameUtils.isValidChannelUrl("http://location:8080/chann/foobar"));
    }

    @Test
    public void testNotChannelUrl() throws Exception {
        assertFalse(ChannelNameUtils.isValidChannelUrl("not a url"));

    }
}
