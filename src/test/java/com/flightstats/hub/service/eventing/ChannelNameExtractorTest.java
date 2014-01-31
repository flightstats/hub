package com.flightstats.hub.service.eventing;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class ChannelNameExtractorTest {

    private ChannelNameExtractor extractor;

    @Before
    public void setUp() throws Exception {
        extractor = new ChannelNameExtractor();
    }

    @Test
	public void testExtractChannelName() throws Exception {
        assertEquals("foobar", extractor.extractFromWS(URI.create("/channel/foobar/ws")));
	}

	@Test
	public void testWhenChannelDoesntMatch() throws Exception {
		String uriString = "/shouldnt/find/me";
		String result = extractor.extractFromWS(URI.create(uriString));
		assertEquals(uriString, result);
	}

    @Test
    public void testExtractChannelUri() throws Exception {
        assertEquals("foobar", ChannelNameExtractor.extractFromChannelUrl(URI.create("http://location:8080/channel/foobar/")));
        assertEquals("foobar", ChannelNameExtractor.extractFromChannelUrl(URI.create("http://location:8080/channel/foobar")));
        assertEquals("foobar", ChannelNameExtractor.extractFromChannelUrl(URI.create("http://hub.svc.prod/channel/foobar")));
    }

    @Test
    public void testExtractChannelString() throws Exception {
        assertEquals("foobar", ChannelNameExtractor.extractFromChannelUrl("http://location:8080/channel/foobar"));
        assertEquals("foobar", ChannelNameExtractor.extractFromChannelUrl("http://location:8080/channel/foobar/"));
        assertEquals("foobar", ChannelNameExtractor.extractFromChannelUrl("http://hub.svc.prod/channel/foobar/"));
    }
}
