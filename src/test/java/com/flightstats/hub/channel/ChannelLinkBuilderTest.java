package com.flightstats.hub.channel;

import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.rest.HalLink;
import com.flightstats.hub.rest.Linked;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChannelLinkBuilderTest {

	private static final String BASE_URL = "http://path.to:8080/";
	private static final String CHANNEL_URL = BASE_URL + "channel";
	private ChannelConfiguration channelConfig;
    private ChannelLinkBuilder linkBuilder;
    private URI channelUri;

    @Before
	public void setup() {
		channelConfig = ChannelConfiguration.builder().withName("spoon").build();
        linkBuilder = new ChannelLinkBuilder();
        channelUri = URI.create(CHANNEL_URL + "/spoon");
    }

	@Test
	public void testBuildChannelUri() throws Exception {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);

        Mockito.when(uriInfo.getRequestUri()).thenReturn(URI.create(CHANNEL_URL));
        Mockito.when(uriInfo.getBaseUri()).thenReturn(URI.create(BASE_URL));

        URI result = linkBuilder.buildChannelUri(channelConfig, uriInfo);
		assertEquals(channelUri, result);
	}

    @Test
    public void testBuildChannelLinks() throws Exception {
        Linked<ChannelConfiguration> linked = linkBuilder.buildChannelLinks(channelConfig, channelUri);
        List<HalLink> halLinks = linked.getHalLinks().getLinks();
        assertEquals(4, halLinks.size());
        assertTrue(halLinks.contains(new HalLink("self", channelUri)));
        assertTrue(halLinks.contains(new HalLink("latest", new URI(channelUri.toString() + "/latest"))));
        assertTrue(halLinks.contains(new HalLink("ws", new URI("ws://path.to:8080/channel/spoon/ws"))));
        assertTrue(halLinks.contains(new HalLink("time", new URI(channelUri.toString() + "/time"))));

    }

}
