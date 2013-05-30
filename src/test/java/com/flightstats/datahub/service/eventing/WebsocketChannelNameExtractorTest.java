package com.flightstats.datahub.service.eventing;

import org.junit.Test;

import java.net.URI;

import static junit.framework.Assert.assertEquals;

public class WebSocketChannelNameExtractorTest {

	@Test
	public void testExtractChannelName() throws Exception {
		//GIVEN
		WebSocketChannelNameExtractor testClass = new WebSocketChannelNameExtractor();
		//WHEN
		String result = testClass.extractChannelName(URI.create("/channel/foobar/ws"));
		//THEN
		assertEquals("foobar", result);
	}

	@Test
	public void testWhenChannelDoesntMatch() throws Exception {
		//GIVEN
		String uriString = "/shouldnt/find/me";
		WebSocketChannelNameExtractor testClass = new WebSocketChannelNameExtractor();
		//WHEN
		String result = testClass.extractChannelName(URI.create(uriString));
		//THEN
		assertEquals(uriString, result);
	}
}
