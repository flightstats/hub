package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.HalLinks;
import com.flightstats.rest.Linked;
import com.google.common.collect.Multimap;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelResourceTest {

    private final static Logger logger = LoggerFactory.getLogger(ChannelResourceTest.class);

    //todo - gfm - 4/29/14 - useful?
	/*@Test
	public void testChannelCreation() throws Exception {
		String channelName = "UHF";

        ChannelConfiguration channelCreationRequest = ChannelConfiguration.builder().withName(channelName).build();
		Date date = new Date();
		ChannelConfiguration channelConfiguration = ChannelConfiguration.builder()
                .withName(channelName).withCreationDate(date).build();
		String channelUri = "http://path/to/UHF";
		String latestUri = "http://path/to/UHF/latest";
		String wsUri = "ws://path/to/UHF/ws";
		Linked<ChannelConfiguration> expected = Linked.linked(channelConfiguration)
				.withLink("self", channelUri)
				.withLink("latest", latestUri)
				.withLink("ws", wsUri)
				.build();
		UriInfo uriInfo = mock(UriInfo.class);
		ChannelService channelService = mock(ChannelService.class);
        ChannelLinkBuilder linkBuilder = mock(ChannelLinkBuilder.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to"));
		when(channelService.channelExists(channelName)).thenReturn(false);
		when(channelService.createChannel(channelConfiguration)).thenReturn(channelConfiguration);
		when(linkBuilder.buildChannelUri(channelConfiguration, uriInfo)).thenReturn(URI.create(channelUri));
		when(linkBuilder.buildChannelLinks(channelConfiguration, URI.create(channelUri))).thenReturn(expected);

        ChannelResource testClass = new ChannelResource(channelService, linkBuilder, uriInfo);

		Response response = testClass.createChannel(channelCreationRequest);

		verify(channelService).createChannel(channelConfiguration);

		assertEquals(201, response.getStatus());
		assertEquals(new URI(channelUri), response.getMetadata().getFirst("location"));
		assertEquals(expected, response.getEntity());
	}*/

	@Test
	public void testGetChannels() throws Exception {
		//GIVEN
		ChannelConfiguration channel1 =  ChannelConfiguration.builder().withName("foo").build();
		ChannelConfiguration channel2 = ChannelConfiguration.builder().withName("bar").build();
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel1, channel2);
		String channel1Uri = "http://superfoo";
		String channel2Uri = "http://superbar";
		String requestUri = "http://datahüb/channel";

		ChannelService channelService = mock(ChannelService.class);
		UriInfo uriInfo = mock(UriInfo.class);
		ChannelLinkBuilder linkBuilder = mock(ChannelLinkBuilder.class);

		ChannelResource testClass = new ChannelResource(channelService, linkBuilder, uriInfo);

		//WHEN
		when(channelService.getChannels()).thenReturn(channels);
		when(linkBuilder.buildChannelUri(channel1.getName(), uriInfo)).thenReturn(URI.create(channel1Uri));
		when(linkBuilder.buildChannelUri(channel2.getName(), uriInfo)).thenReturn(URI.create(channel2Uri));
		when(uriInfo.getRequestUri()).thenReturn(URI.create(requestUri));

		Response result = testClass.getChannels();

		//THEN

		Linked<Map> resultEntity = (Linked<Map>) result.getEntity();
		HalLinks resultHalLinks = resultEntity.getHalLinks();
		List<HalLink> links = resultHalLinks.getLinks();
		assertNull(resultEntity.getObject());
		assertEquals(1, links.size());
		assertEquals(new HalLink("self", URI.create(requestUri)), links.get(0));

		Multimap<String, HalLink> resultMultiLinks = resultHalLinks.getMultiLinks();
		assertEquals(1, resultMultiLinks.keySet().size());
		assertEquals(2, resultMultiLinks.size());
		Collection<HalLink> resultChannelLinks = resultMultiLinks.asMap().get("channels");
		assertThat(resultChannelLinks, CoreMatchers.hasItems(new HalLink(channel1.getName(), URI.create(channel1Uri)),
                new HalLink(channel2.getName(), URI.create(channel2Uri))));
	}

}
