package com.flightstats.hub.channel;

import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertFalse;

public class ChannelContentResourceTest {

    @Test
    public void testHttpURLConnection() {
        String accept = "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";
        assertFalse(ChannelContentResource.contentTypeIsNotCompatible(accept, MediaType.WILDCARD_TYPE));
    }

    @Test
    public void testMost() {
        String accept = "text/html, image/gif, image/jpeg, */*; q=.2";
        assertFalse(ChannelContentResource.contentTypeIsNotCompatible(accept, MediaType.WILDCARD_TYPE));
    }

}