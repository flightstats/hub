package com.flightstats.hub.channel;

import com.flightstats.hub.test.TestMain;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertFalse;

public class ChannelContentResourceTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = TestMain.start();
    }

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